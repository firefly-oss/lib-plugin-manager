package com.catalis.core.plugin.event;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Utility class for serializing and deserializing plugin events.
 * This class configures Jackson to handle polymorphic event types.
 */
@Component
public class PluginEventSerializer {
    
    private static final Logger logger = LoggerFactory.getLogger(PluginEventSerializer.class);
    
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;
    
    /**
     * Creates a new PluginEventSerializer.
     * 
     * @param objectMapper the object mapper
     * @param applicationContext the application context
     */
    @Autowired
    public PluginEventSerializer(ObjectMapper objectMapper, ApplicationContext applicationContext) {
        this.objectMapper = objectMapper;
        this.applicationContext = applicationContext;
    }
    
    /**
     * Initializes the serializer.
     */
    @PostConstruct
    public void init() {
        // Configure the object mapper for polymorphic deserialization
        objectMapper.registerModule(new JavaTimeModule());
        
        // Add type info to PluginEvent class
        objectMapper.addMixIn(PluginEvent.class, PluginEventMixin.class);
        
        // Register all PluginEvent subclasses
        registerPluginEventSubclasses();
        
        logger.info("PluginEventSerializer initialized");
    }
    
    /**
     * Registers all PluginEvent subclasses with the object mapper.
     */
    private void registerPluginEventSubclasses() {
        // Find all beans that are subclasses of PluginEvent
        Map<String, PluginEvent> eventBeans = applicationContext.getBeansOfType(PluginEvent.class);
        
        // Register each event type
        for (PluginEvent event : eventBeans.values()) {
            Class<? extends PluginEvent> eventClass = event.getClass();
            String eventType = event.getEventType();
            
            objectMapper.registerSubtypes(new NamedType(eventClass, eventType));
            logger.debug("Registered event type: {} -> {}", eventType, eventClass.getName());
        }
        
        // Register built-in event types
        objectMapper.registerSubtypes(
                new NamedType(PluginLifecycleEvent.class, "LIFECYCLE"),
                new NamedType(PluginConfigurationEvent.class, "CONFIGURATION")
        );
        
        logger.debug("Registered built-in event types");
    }
    
    /**
     * Gets the configured object mapper.
     * 
     * @return the object mapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
    /**
     * Jackson mixin to add type information to PluginEvent.
     */
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
    private abstract static class PluginEventMixin {
    }
}
