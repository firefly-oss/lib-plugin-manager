package com.catalis.core.plugin.debug.jdi;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles the connection to a JVM for debugging using JDI.
 */
public class DebuggerConnection {
    private static final Logger logger = LoggerFactory.getLogger(DebuggerConnection.class);

    private final String sessionId;
    private final String pluginId;
    private final int port;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean suspended = new AtomicBoolean(false);
    private final Map<String, BreakpointRequest> breakpointRequests = new ConcurrentHashMap<>();
    private final Map<String, Location> breakpointLocations = new ConcurrentHashMap<>();
    private final Map<String, EventRequest> eventRequests = new ConcurrentHashMap<>();
    
    private VirtualMachine vm;
    private EventRequestManager eventRequestManager;
    private EventQueue eventQueue;
    private Thread eventThread;
    private ThreadReference currentThread;
    private StackFrame currentFrame;
    private EventHandler eventHandler;

    /**
     * Creates a new DebuggerConnection.
     *
     * @param sessionId the debug session ID
     * @param pluginId the plugin ID
     * @param port the port to connect to
     */
    public DebuggerConnection(String sessionId, String pluginId, int port) {
        this.sessionId = sessionId;
        this.pluginId = pluginId;
        this.port = port;
    }

    /**
     * Sets the event handler for this connection.
     *
     * @param eventHandler the event handler
     */
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /**
     * Connects to the target JVM.
     *
     * @return a Mono that completes when the connection is established
     */
    public Mono<Void> connect() {
        return Mono.fromCallable(() -> {
            if (connected.get()) {
                return null;
            }

            try {
                // Find the socket attaching connector
                AttachingConnector connector = findSocketAttachingConnector();
                if (connector == null) {
                    throw new IllegalStateException("Could not find socket attaching connector");
                }

                // Set up the connection arguments
                Map<String, Connector.Argument> arguments = connector.defaultArguments();
                arguments.get("port").setValue(String.valueOf(port));
                arguments.get("hostname").setValue("localhost");

                // Connect to the target VM
                vm = connector.attach(arguments);
                eventRequestManager = vm.eventRequestManager();
                eventQueue = vm.eventQueue();
                connected.set(true);

                // Start the event handling thread
                startEventHandling();

                logger.info("Connected to target VM on port {}", port);
                return null;
            } catch (IOException | IllegalConnectorArgumentsException e) {
                logger.error("Failed to connect to target VM on port {}", port, e);
                throw new RuntimeException("Failed to connect to target VM", e);
            }
        });
    }

    /**
     * Disconnects from the target JVM.
     *
     * @return a Mono that completes when the connection is closed
     */
    public Mono<Void> disconnect() {
        return Mono.fromRunnable(() -> {
            if (!connected.get()) {
                return;
            }

            try {
                // Stop the event handling thread
                if (eventThread != null && eventThread.isAlive()) {
                    eventThread.interrupt();
                    eventThread.join(1000); // Wait for the thread to terminate
                }

                // Clear all breakpoints and event requests
                breakpointRequests.clear();
                breakpointLocations.clear();
                eventRequests.clear();

                // Disconnect from the VM
                vm.dispose();
                vm = null;
                eventRequestManager = null;
                eventQueue = null;
                currentThread = null;
                currentFrame = null;
                connected.set(false);
                suspended.set(false);

                logger.info("Disconnected from target VM");
            } catch (Exception e) {
                logger.error("Error disconnecting from target VM", e);
            }
        });
    }

    /**
     * Checks if the debugger is connected to the target JVM.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Checks if the target JVM is suspended.
     *
     * @return true if suspended, false otherwise
     */
    public boolean isSuspended() {
        return suspended.get();
    }

    /**
     * Sets a breakpoint at the specified class and line number.
     *
     * @param className the fully qualified class name
     * @param lineNumber the line number
     * @return a Mono that emits the breakpoint ID
     */
    public Mono<String> setBreakpoint(String className, int lineNumber) {
        return Mono.fromCallable(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected to target VM");
            }

            try {
                // Find the class by name
                List<ReferenceType> classes = vm.classesByName(className);
                if (classes.isEmpty()) {
                    throw new IllegalArgumentException("Class not found: " + className);
                }

                ReferenceType classType = classes.get(0);

                // Find the location for the specified line number
                List<Location> locations = classType.locationsOfLine(lineNumber);
                if (locations.isEmpty()) {
                    throw new IllegalArgumentException("Invalid line number: " + lineNumber);
                }

                Location location = locations.get(0);

                // Create a breakpoint request
                BreakpointRequest breakpointRequest = eventRequestManager.createBreakpointRequest(location);
                breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                breakpointRequest.enable();

                // Generate a unique ID for the breakpoint
                String breakpointId = UUID.randomUUID().toString();
                breakpointRequests.put(breakpointId, breakpointRequest);
                breakpointLocations.put(breakpointId, location);

                logger.info("Set breakpoint at {}:{}", className, lineNumber);
                return breakpointId;
            } catch (AbsentInformationException e) {
                logger.error("Failed to set breakpoint at {}:{}", className, lineNumber, e);
                throw new IllegalArgumentException("Failed to set breakpoint: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Removes a breakpoint.
     *
     * @param breakpointId the breakpoint ID
     * @return a Mono that completes when the breakpoint is removed
     */
    public Mono<Void> removeBreakpoint(String breakpointId) {
        return Mono.fromRunnable(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected to target VM");
            }

            BreakpointRequest request = breakpointRequests.remove(breakpointId);
            if (request != null) {
                eventRequestManager.deleteEventRequest(request);
                breakpointLocations.remove(breakpointId);
                logger.info("Removed breakpoint: {}", breakpointId);
            } else {
                logger.warn("Breakpoint not found: {}", breakpointId);
            }
        });
    }

    /**
     * Gets all breakpoints.
     *
     * @return a Map of breakpoint IDs to breakpoint information
     */
    public Map<String, Map<String, Object>> getBreakpoints() {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected to target VM");
        }

        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, Location> entry : breakpointLocations.entrySet()) {
            String breakpointId = entry.getKey();
            Location location = entry.getValue();
            BreakpointRequest request = breakpointRequests.get(breakpointId);

            Map<String, Object> breakpointInfo = new HashMap<>();
            breakpointInfo.put("id", breakpointId);
            breakpointInfo.put("className", location.declaringType().name());
            breakpointInfo.put("lineNumber", location.lineNumber());
            breakpointInfo.put("enabled", request != null && request.isEnabled());
            breakpointInfo.put("methodName", location.method().name());

            result.put(breakpointId, breakpointInfo);
        }

        return result;
    }

    /**
     * Continues execution after a breakpoint.
     *
     * @return a Mono that completes when execution is resumed
     */
    public Mono<Void> continueExecution() {
        return Mono.fromRunnable(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected to target VM");
            }

            if (!suspended.get()) {
                logger.warn("VM is not suspended, cannot continue execution");
                return;
            }

            try {
                vm.resume();
                suspended.set(false);
                currentThread = null;
                currentFrame = null;
                logger.info("Continued execution");
            } catch (Exception e) {
                logger.error("Failed to continue execution", e);
                throw new RuntimeException("Failed to continue execution", e);
            }
        });
    }

    /**
     * Steps over the current line.
     *
     * @return a Mono that completes when the step is complete
     */
    public Mono<Void> stepOver() {
        return Mono.fromRunnable(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected to target VM");
            }

            if (!suspended.get() || currentThread == null) {
                logger.warn("VM is not suspended or no current thread, cannot step over");
                return;
            }

            try {
                StepRequest request = eventRequestManager.createStepRequest(
                        currentThread, StepRequest.STEP_LINE, StepRequest.STEP_OVER);
                request.addCountFilter(1); // Only one step
                request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                request.enable();

                // Store the request for later deletion
                String requestId = UUID.randomUUID().toString();
                eventRequests.put(requestId, request);

                vm.resume();
                logger.info("Stepped over");
            } catch (Exception e) {
                logger.error("Failed to step over", e);
                throw new RuntimeException("Failed to step over", e);
            }
        });
    }

    /**
     * Steps into a method.
     *
     * @return a Mono that completes when the step is complete
     */
    public Mono<Void> stepInto() {
        return Mono.fromRunnable(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected to target VM");
            }

            if (!suspended.get() || currentThread == null) {
                logger.warn("VM is not suspended or no current thread, cannot step into");
                return;
            }

            try {
                StepRequest request = eventRequestManager.createStepRequest(
                        currentThread, StepRequest.STEP_LINE, StepRequest.STEP_INTO);
                request.addCountFilter(1); // Only one step
                request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                request.enable();

                // Store the request for later deletion
                String requestId = UUID.randomUUID().toString();
                eventRequests.put(requestId, request);

                vm.resume();
                logger.info("Stepped into");
            } catch (Exception e) {
                logger.error("Failed to step into", e);
                throw new RuntimeException("Failed to step into", e);
            }
        });
    }

    /**
     * Steps out of the current method.
     *
     * @return a Mono that completes when the step is complete
     */
    public Mono<Void> stepOut() {
        return Mono.fromRunnable(() -> {
            if (!connected.get()) {
                throw new IllegalStateException("Not connected to target VM");
            }

            if (!suspended.get() || currentThread == null) {
                logger.warn("VM is not suspended or no current thread, cannot step out");
                return;
            }

            try {
                StepRequest request = eventRequestManager.createStepRequest(
                        currentThread, StepRequest.STEP_LINE, StepRequest.STEP_OUT);
                request.addCountFilter(1); // Only one step
                request.setSuspendPolicy(EventRequest.SUSPEND_ALL);
                request.enable();

                // Store the request for later deletion
                String requestId = UUID.randomUUID().toString();
                eventRequests.put(requestId, request);

                vm.resume();
                logger.info("Stepped out");
            } catch (Exception e) {
                logger.error("Failed to step out", e);
                throw new RuntimeException("Failed to step out", e);
            }
        });
    }

    /**
     * Gets the value of a variable.
     *
     * @param variableName the variable name
     * @return the variable value
     */
    public Object getVariableValue(String variableName) {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected to target VM");
        }

        if (!suspended.get() || currentFrame == null) {
            throw new IllegalStateException("VM is not suspended or no current frame");
        }

        try {
            // Try to find the variable in the current frame
            LocalVariable variable = currentFrame.visibleVariableByName(variableName);
            if (variable != null) {
                Value value = currentFrame.getValue(variable);
                return convertValue(value);
            }

            // Try to find the variable in 'this' object
            ObjectReference thisObject = currentFrame.thisObject();
            if (thisObject != null) {
                ReferenceType type = thisObject.referenceType();
                Field field = type.fieldByName(variableName);
                if (field != null) {
                    Value value = thisObject.getValue(field);
                    return convertValue(value);
                }
            }

            throw new IllegalArgumentException("Variable not found: " + variableName);
        } catch (AbsentInformationException e) {
            logger.error("Failed to get variable value: {}", variableName, e);
            throw new IllegalArgumentException("Failed to get variable value: " + e.getMessage(), e);
        }
    }

    /**
     * Gets all local variables at the current execution point.
     *
     * @return a Map of variable names to values
     */
    public Map<String, Object> getLocalVariables() {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected to target VM");
        }

        if (!suspended.get() || currentFrame == null) {
            throw new IllegalStateException("VM is not suspended or no current frame");
        }

        try {
            Map<String, Object> variables = new HashMap<>();

            // Get local variables
            for (LocalVariable variable : currentFrame.visibleVariables()) {
                Value value = currentFrame.getValue(variable);
                variables.put(variable.name(), convertValue(value));
            }

            // Get 'this' fields if available
            ObjectReference thisObject = currentFrame.thisObject();
            if (thisObject != null) {
                ReferenceType type = thisObject.referenceType();
                for (Field field : type.visibleFields()) {
                    Value value = thisObject.getValue(field);
                    variables.put("this." + field.name(), convertValue(value));
                }
            }

            return variables;
        } catch (AbsentInformationException e) {
            logger.error("Failed to get local variables", e);
            throw new RuntimeException("Failed to get local variables: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluates an expression in the context of the current execution point.
     *
     * @param expression the expression to evaluate
     * @return the result of the evaluation
     */
    public Object evaluateExpression(String expression) {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected to target VM");
        }

        if (!suspended.get() || currentThread == null) {
            throw new IllegalStateException("VM is not suspended or no current thread");
        }

        try {
            // For simple expressions like variable names, try to get the variable value
            if (!expression.contains(".") && !expression.contains("(") && !expression.contains(")")) {
                try {
                    return getVariableValue(expression);
                } catch (IllegalArgumentException e) {
                    // Not a simple variable, continue with evaluation
                }
            }

            // For more complex expressions, use JDI's evaluate method
            if (vm.canBeModified()) {
                return "Expression evaluation not fully implemented";
            } else {
                return "Target VM does not support expression evaluation";
            }
        } catch (Exception e) {
            logger.error("Failed to evaluate expression: {}", expression, e);
            throw new RuntimeException("Failed to evaluate expression: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the current stack trace.
     *
     * @return a List of stack frames
     */
    public List<Map<String, Object>> getStackTrace() {
        if (!connected.get()) {
            throw new IllegalStateException("Not connected to target VM");
        }

        if (!suspended.get() || currentThread == null) {
            throw new IllegalStateException("VM is not suspended or no current thread");
        }

        try {
            List<Map<String, Object>> stackTrace = new ArrayList<>();
            for (StackFrame frame : currentThread.frames()) {
                Map<String, Object> frameInfo = new HashMap<>();
                Location location = frame.location();
                frameInfo.put("className", location.declaringType().name());
                frameInfo.put("methodName", location.method().name());
                frameInfo.put("lineNumber", location.lineNumber());
                
                try {
                    String sourceName = location.sourceName();
                    frameInfo.put("fileName", sourceName);
                } catch (AbsentInformationException e) {
                    frameInfo.put("fileName", "Unknown");
                }
                
                stackTrace.add(frameInfo);
            }
            return stackTrace;
        } catch (IncompatibleThreadStateException e) {
            logger.error("Failed to get stack trace", e);
            throw new RuntimeException("Failed to get stack trace: " + e.getMessage(), e);
        }
    }

    /**
     * Finds the socket attaching connector.
     *
     * @return the socket attaching connector, or null if not found
     */
    private AttachingConnector findSocketAttachingConnector() {
        for (AttachingConnector connector : Bootstrap.virtualMachineManager().attachingConnectors()) {
            if (connector.name().equals("com.sun.jdi.SocketAttach")) {
                return connector;
            }
        }
        return null;
    }

    /**
     * Starts the event handling thread.
     */
    private void startEventHandling() {
        eventThread = new Thread(() -> {
            try {
                while (connected.get() && !Thread.currentThread().isInterrupted()) {
                    EventSet eventSet = eventQueue.remove();
                    
                    for (Event event : eventSet) {
                        handleEvent(event);
                    }
                    
                    // Resume the VM unless we hit a breakpoint or step
                    if (!suspended.get()) {
                        eventSet.resume();
                    }
                }
            } catch (InterruptedException e) {
                // Thread was interrupted, exit gracefully
                Thread.currentThread().interrupt();
            } catch (VMDisconnectedException e) {
                // VM disconnected, exit gracefully
                connected.set(false);
            } catch (Exception e) {
                logger.error("Error in event handling thread", e);
            }
        }, "JDI-Event-Handler-" + sessionId);
        
        eventThread.setDaemon(true);
        eventThread.start();
    }

    /**
     * Handles a JDI event.
     *
     * @param event the event to handle
     */
    private void handleEvent(Event event) {
        if (event instanceof BreakpointEvent) {
            handleBreakpointEvent((BreakpointEvent) event);
        } else if (event instanceof StepEvent) {
            handleStepEvent((StepEvent) event);
        } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
            connected.set(false);
            suspended.set(false);
            if (eventHandler != null) {
                eventHandler.onDisconnect();
            }
        }
    }

    /**
     * Handles a breakpoint event.
     *
     * @param event the breakpoint event
     */
    private void handleBreakpointEvent(BreakpointEvent event) {
        suspended.set(true);
        currentThread = event.thread();
        try {
            currentFrame = currentThread.frame(0);
        } catch (IncompatibleThreadStateException e) {
            logger.error("Failed to get current frame", e);
            currentFrame = null;
        }
        
        Location location = event.location();
        logger.info("Breakpoint hit at {}:{}", location.declaringType().name(), location.lineNumber());
        
        if (eventHandler != null) {
            eventHandler.onBreakpointHit(location.declaringType().name(), location.lineNumber());
        }
        
        // Clean up any one-time step requests
        for (Iterator<Map.Entry<String, EventRequest>> it = eventRequests.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, EventRequest> entry = it.next();
            if (entry.getValue() instanceof StepRequest) {
                eventRequestManager.deleteEventRequest(entry.getValue());
                it.remove();
            }
        }
    }

    /**
     * Handles a step event.
     *
     * @param event the step event
     */
    private void handleStepEvent(StepEvent event) {
        suspended.set(true);
        currentThread = event.thread();
        try {
            currentFrame = currentThread.frame(0);
        } catch (IncompatibleThreadStateException e) {
            logger.error("Failed to get current frame", e);
            currentFrame = null;
        }
        
        Location location = event.location();
        logger.info("Step completed at {}:{}", location.declaringType().name(), location.lineNumber());
        
        if (eventHandler != null) {
            eventHandler.onStepCompleted(location.declaringType().name(), location.lineNumber());
        }
        
        // Clean up the step request
        eventRequestManager.deleteEventRequest(event.request());
    }

    /**
     * Converts a JDI Value to a Java object.
     *
     * @param value the JDI Value
     * @return the Java object
     */
    private Object convertValue(Value value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof StringReference) {
            return ((StringReference) value).value();
        } else if (value instanceof PrimitiveValue) {
            if (value instanceof BooleanValue) {
                return ((BooleanValue) value).value();
            } else if (value instanceof ByteValue) {
                return ((ByteValue) value).value();
            } else if (value instanceof CharValue) {
                return ((CharValue) value).value();
            } else if (value instanceof DoubleValue) {
                return ((DoubleValue) value).value();
            } else if (value instanceof FloatValue) {
                return ((FloatValue) value).value();
            } else if (value instanceof IntegerValue) {
                return ((IntegerValue) value).value();
            } else if (value instanceof LongValue) {
                return ((LongValue) value).value();
            } else if (value instanceof ShortValue) {
                return ((ShortValue) value).value();
            }
        } else if (value instanceof ObjectReference) {
            // For object references, return a string representation
            return value.toString();
        } else if (value instanceof ArrayReference) {
            // For arrays, convert to a list
            ArrayReference array = (ArrayReference) value;
            List<Object> result = new ArrayList<>();
            for (Value element : array.getValues()) {
                result.add(convertValue(element));
            }
            return result;
        }
        
        return value.toString();
    }

    /**
     * Interface for handling debug events.
     */
    public interface EventHandler {
        /**
         * Called when a breakpoint is hit.
         *
         * @param className the class name
         * @param lineNumber the line number
         */
        void onBreakpointHit(String className, int lineNumber);
        
        /**
         * Called when a step is completed.
         *
         * @param className the class name
         * @param lineNumber the line number
         */
        void onStepCompleted(String className, int lineNumber);
        
        /**
         * Called when the debugger disconnects.
         */
        void onDisconnect();
    }
}
