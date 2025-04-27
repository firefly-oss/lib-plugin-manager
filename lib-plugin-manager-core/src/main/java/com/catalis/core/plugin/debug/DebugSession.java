package com.catalis.core.plugin.debug;

import com.catalis.core.plugin.model.PluginDebugEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a debug session for a plugin.
 */
public class DebugSession {

    private final String id;
    private final String pluginId;
    private final Instant startTime;
    private Instant lastActivityTime;
    private boolean active;
    private final Map<String, Breakpoint> breakpoints = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionData = new ConcurrentHashMap<>();

    // JDI debugging properties
    private int debugPort;
    private boolean jdiConnected;
    private String currentClassName;
    private int currentLineNumber;

    /**
     * Creates a new debug session for a plugin.
     *
     * @param pluginId the plugin ID
     */
    public DebugSession(String pluginId) {
        this.id = UUID.randomUUID().toString();
        this.pluginId = pluginId;
        this.startTime = Instant.now();
        this.lastActivityTime = startTime;
        this.active = true;
        this.debugPort = -1;
        this.jdiConnected = false;
        this.currentClassName = null;
        this.currentLineNumber = -1;
    }

    /**
     * Creates a new debug session for a plugin with a specific debug port.
     *
     * @param pluginId the plugin ID
     * @param debugPort the debug port
     */
    public DebugSession(String pluginId, int debugPort) {
        this(pluginId);
        this.debugPort = debugPort;
    }

    /**
     * Gets the session ID.
     *
     * @return the session ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the plugin ID.
     *
     * @return the plugin ID
     */
    public String getPluginId() {
        return pluginId;
    }

    /**
     * Gets the start time of the session.
     *
     * @return the start time
     */
    public Instant getStartTime() {
        return startTime;
    }

    /**
     * Gets the time of the last activity in the session.
     *
     * @return the last activity time
     */
    public Instant getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Updates the last activity time to the current time.
     */
    public void updateLastActivityTime() {
        this.lastActivityTime = Instant.now();
    }

    /**
     * Checks if the session is active.
     *
     * @return true if the session is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Sets whether the session is active.
     *
     * @param active whether the session is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Adds a breakpoint to the session.
     *
     * @param className the fully qualified class name
     * @param lineNumber the line number
     * @return the breakpoint ID
     */
    public String addBreakpoint(String className, int lineNumber) {
        Breakpoint breakpoint = new Breakpoint(className, lineNumber);
        breakpoints.put(breakpoint.getId(), breakpoint);
        updateLastActivityTime();
        return breakpoint.getId();
    }

    /**
     * Removes a breakpoint from the session.
     *
     * @param breakpointId the breakpoint ID
     * @return true if the breakpoint was removed, false if it wasn't found
     */
    public boolean removeBreakpoint(String breakpointId) {
        updateLastActivityTime();
        return breakpoints.remove(breakpointId) != null;
    }

    /**
     * Gets all breakpoints in the session.
     *
     * @return a map of breakpoint IDs to breakpoints
     */
    public Map<String, Breakpoint> getBreakpoints() {
        updateLastActivityTime();
        return new HashMap<>(breakpoints);
    }

    /**
     * Gets a breakpoint by its ID.
     *
     * @param breakpointId the breakpoint ID
     * @return the breakpoint, or null if not found
     */
    public Breakpoint getBreakpoint(String breakpointId) {
        updateLastActivityTime();
        return breakpoints.get(breakpointId);
    }

    /**
     * Stores data in the session.
     *
     * @param key the data key
     * @param value the data value
     */
    public void putData(String key, Object value) {
        sessionData.put(key, value);
        updateLastActivityTime();
    }

    /**
     * Gets data from the session.
     *
     * @param key the data key
     * @return the data value, or null if not found
     */
    public Object getData(String key) {
        updateLastActivityTime();
        return sessionData.get(key);
    }

    /**
     * Removes data from the session.
     *
     * @param key the data key
     * @return the removed data value, or null if not found
     */
    public Object removeData(String key) {
        updateLastActivityTime();
        return sessionData.remove(key);
    }

    /**
     * Gets the debug port for this session.
     *
     * @return the debug port, or -1 if not set
     */
    public int getDebugPort() {
        return debugPort;
    }

    /**
     * Sets the debug port for this session.
     *
     * @param debugPort the debug port
     */
    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
        updateLastActivityTime();
    }

    /**
     * Checks if the session is connected to the JDI.
     *
     * @return true if connected, false otherwise
     */
    public boolean isJdiConnected() {
        return jdiConnected;
    }

    /**
     * Sets whether the session is connected to the JDI.
     *
     * @param jdiConnected whether the session is connected
     */
    public void setJdiConnected(boolean jdiConnected) {
        this.jdiConnected = jdiConnected;
        updateLastActivityTime();
    }

    /**
     * Gets the current class name.
     *
     * @return the current class name, or null if not set
     */
    public String getCurrentClassName() {
        return currentClassName;
    }

    /**
     * Sets the current class name.
     *
     * @param currentClassName the current class name
     */
    public void setCurrentClassName(String currentClassName) {
        this.currentClassName = currentClassName;
        updateLastActivityTime();
    }

    /**
     * Gets the current line number.
     *
     * @return the current line number, or -1 if not set
     */
    public int getCurrentLineNumber() {
        return currentLineNumber;
    }

    /**
     * Sets the current line number.
     *
     * @param currentLineNumber the current line number
     */
    public void setCurrentLineNumber(int currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
        updateLastActivityTime();
    }


    /**
     * Represents a breakpoint in a debug session.
     */
    public static class Breakpoint {
        private final String id;
        private final String className;
        private final int lineNumber;
        private boolean enabled;
        private final Instant creationTime;

        /**
         * Creates a new breakpoint.
         *
         * @param className the fully qualified class name
         * @param lineNumber the line number
         */
        public Breakpoint(String className, int lineNumber) {
            this.id = UUID.randomUUID().toString();
            this.className = className;
            this.lineNumber = lineNumber;
            this.enabled = true;
            this.creationTime = Instant.now();
        }

        /**
         * Gets the breakpoint ID.
         *
         * @return the breakpoint ID
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the fully qualified class name.
         *
         * @return the class name
         */
        public String getClassName() {
            return className;
        }

        /**
         * Gets the line number.
         *
         * @return the line number
         */
        public int getLineNumber() {
            return lineNumber;
        }

        /**
         * Checks if the breakpoint is enabled.
         *
         * @return true if the breakpoint is enabled, false otherwise
         */
        public boolean isEnabled() {
            return enabled;
        }

        /**
         * Sets whether the breakpoint is enabled.
         *
         * @param enabled whether the breakpoint is enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Gets the creation time of the breakpoint.
         *
         * @return the creation time
         */
        public Instant getCreationTime() {
            return creationTime;
        }

        /**
         * Converts the breakpoint to a map for serialization.
         *
         * @return a map representation of the breakpoint
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("id", id);
            map.put("className", className);
            map.put("lineNumber", lineNumber);
            map.put("enabled", enabled);
            map.put("creationTime", creationTime);
            return map;
        }
    }
}
