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

import java.lang.reflect.Proxy;

import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.annotation.RegistryType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;

/**
 * Registers a MetricRegistryProxy bean for the BASE scope
 */
@ApplicationScoped
public class MetricRegistryProvider {

    @Produces
    @RegistryType(type = Type.BASE)
    public MetricRegistryProxy getBaseRegistry() {
        Object metricRegistry =
                CDI.current().select(MetricRegistryProxyHandler.METRIC_REGISTRY_CLAZZ, RegistryTypeLiteral.BASE).get();
        return getProxy(metricRegistry);
    }

    private MetricRegistryProxy getProxy(Object metricRegistry) {
        MetricRegistryProxyHandler handler = new MetricRegistryProxyHandler(metricRegistry);
        ClassLoader cl = MetricRegistryProvider.class.getClassLoader();
        return (MetricRegistryProxy) Proxy.newProxyInstance(cl, new Class<?>[]{MetricRegistryProxy.class}, handler);

    }

}
