/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.firefly.core.plugin.annotation;

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
