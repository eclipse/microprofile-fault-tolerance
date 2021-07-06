/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.eclipse.microprofile.fault.tolerance.tck.metrics.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invocation handler which dynamically loads the MetricRegistry class and invokes its methods by reflection
 */
public class MetricRegistryProxyHandler implements InvocationHandler {

    private static final String METRIC_REGISTRY_CLASS_NAME = "org.eclipse.microprofile.metrics.MetricRegistry";

    public static final Class<?> METRIC_REGISTRY_CLAZZ;

    private final Object metricRegistryInstance;

    static {
        try {
            METRIC_REGISTRY_CLAZZ = Class.forName(METRIC_REGISTRY_CLASS_NAME);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to find MetricRegistry class", e);
        }
    }

    public MetricRegistryProxyHandler(Object metricRegistryInstance) {
        METRIC_REGISTRY_CLAZZ.cast(metricRegistryInstance); // Check instance is correct class
        this.metricRegistryInstance = metricRegistryInstance;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method realMethod = METRIC_REGISTRY_CLAZZ.getMethod(method.getName(), method.getParameterTypes());
        try {
            return realMethod.invoke(metricRegistryInstance, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

}
