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


package com.firefly.core.plugin.model;

/**
 * Represents the possible states of a plugin in its lifecycle.
 */
public enum PluginState {
    /**
     * The plugin has been installed but not yet initialized.
     */
    INSTALLED,
    
    /**
     * The plugin has been initialized but not yet started.
     */
    INITIALIZED,
    
    /**
     * The plugin is currently running.
     */
    STARTED,
    
    /**
     * The plugin has been stopped but is still initialized.
     */
    STOPPED,
    
    /**
     * The plugin has encountered an error and is in a failed state.
     */
    FAILED,
    
    /**
     * The plugin has been uninstalled.
     */
    UNINSTALLED
}
