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

package com.dtolabs.rundeck.core.execution.component;

/**
 * Defines a component that can be attached to the execution context
 */
public interface ContextComponent<T> {
    /**
     * name
     */
    String getName();

    Class<T> getType();

    T getObject();

    /**
     * @return true if the component should not be used more than once
     */
    default boolean isUseOnce() {
        return false;
    }

    /**
     * Create a component
     *
     * @param name
     * @param o
     * @param type
     * @param <T>
     */
    static <T> ContextComponent<T> with(String name, T o, Class<T> type) {
        return with(name, o, type, false);
    }

    /**
     * Create a component
     *
     * @param name
     * @param o
     * @param type
     * @param useOnce true for single use component
     * @param <T>
     */
    static <T> ContextComponent<T> with(String name, T o, Class<T> type, boolean useOnce) {
        return new ContextComponent<T>() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public T getObject() {
                return o;
            }

            @Override
            public boolean isUseOnce() {
                return useOnce;
            }
        };
    }

    static boolean equalsTo(ContextComponent acomp, ContextComponent bcomp) {
        return bcomp.getName().equals(acomp.getName())
               && bcomp.getType().equals(acomp.getType());
    }
}
