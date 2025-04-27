package com.catalis.core.plugin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as a Firefly plugin.
 * This annotation is used to identify plugin classes during component scanning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {
    
    /**
     * The unique identifier for the plugin.
     * This ID must be unique across all plugins in the system.
     * 
     * @return the plugin ID
     */
    String id();
    
    /**
     * The human-readable name of the plugin.
     * 
     * @return the plugin name
     */
    String name();
    
    /**
     * The plugin version in semantic versioning format (e.g., 1.0.0).
     * 
     * @return the plugin version
     */
    String version();
    
    /**
     * A brief description of the plugin's functionality.
     * 
     * @return the plugin description
     */
    String description() default "";
    
    /**
     * The author or organization that created the plugin.
     * 
     * @return the plugin author
     */
    String author() default "";
    
    /**
     * The minimum required version of the Firefly platform.
     * 
     * @return the minimum platform version
     */
    String minPlatformVersion() default "1.0.0";
    
    /**
     * The maximum compatible version of the Firefly platform.
     * 
     * @return the maximum platform version
     */
    String maxPlatformVersion() default "";
    
    /**
     * IDs of other plugins that this plugin depends on.
     * 
     * @return array of plugin IDs that this plugin depends on
     */
    String[] dependencies() default {};
}
