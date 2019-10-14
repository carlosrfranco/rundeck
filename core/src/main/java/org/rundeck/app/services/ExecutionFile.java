/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.services;

import java.io.File;

/**
 * Represents a file produced by an execution producer
 */
public interface ExecutionFile {
    File getLocalFile();

    /**
     *
     * @return true if this file should be stored, false if this file does not need to be stored (optional file)
     */
    default boolean isShouldBeStored(){
        return true;
    }

    /**
     * @return policy for deleting this file
     */
    DeletePolicy getFileDeletePolicy();

    enum DeletePolicy {
        ALWAYS,
        /**
         * Never delete the produced file (required for other use)
         */
        NEVER,
        /**
         * Delete only when it can be retrieved again later
         */
        WHEN_RETRIEVABLE
    }
}
