package com.catalis.core.plugin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a class as an implementation of an extension point.
 * This annotation is used to identify extension implementations during component scanning.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Extension {
    
    /**
     * The ID of the extension point that this class implements.
     * 
     * @return the extension point ID
     */
    String extensionPointId();
    
    /**
     * The priority of this extension implementation.
     * Higher values indicate higher priority when multiple implementations exist.
     * 
     * @return the extension priority
     */
    int priority() default 0;
    
    /**
     * A brief description of this extension implementation.
     * 
     * @return the extension description
     */
    String description() default "";
}
