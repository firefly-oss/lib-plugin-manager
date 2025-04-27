package com.catalis.core.plugin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark an interface or abstract class as an extension point.
 * Extension points define the contract that plugins must implement to extend
 * specific functionality in the Firefly platform.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExtensionPoint {
    
    /**
     * The unique identifier for the extension point.
     * 
     * @return the extension point ID
     */
    String id();
    
    /**
     * A brief description of the extension point's purpose.
     * 
     * @return the extension point description
     */
    String description() default "";
    
    /**
     * Indicates whether multiple implementations of this extension point are allowed.
     * 
     * @return true if multiple implementations are allowed, false otherwise
     */
    boolean allowMultiple() default true;
}
