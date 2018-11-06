/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.interceptor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Creates counters that can be injected.
 * <p>
 * Usage:
 * {@code @Inject @CounterId("foo") AtomicInteger fooCounter;}
 * <p>
 * Injection points which specify the same {@code @CounterId} will receive the same counter instance.
 * @author carlosdlr
 */
@ApplicationScoped
public class CounterFactory {


    private final Map<String, AtomicInteger> counters = new HashMap<>();
    private Queue<String> orderQueue = new ConcurrentLinkedQueue<>();


    @Qualifier
    @Retention(RUNTIME)
    @Target({ METHOD, FIELD, PARAMETER })
    public @interface CounterId {
        @Nonbinding
        String value();
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({ METHOD, FIELD, PARAMETER })
    public @interface OrderId {
        @Nonbinding
        String value();
    }

    @Produces
    @Dependent
    @CounterId(value = "")
    private synchronized AtomicInteger produce(InjectionPoint injectionPoint) {
        String id = getAnnotationObject(CounterId.class, injectionPoint).value();

        AtomicInteger counter = counters.get(id);
        if (counter == null) {
            counter = new AtomicInteger();
            counters.put(id, counter);
        }

        return counter;
    }

    @Produces
    @Dependent
    @OrderId(value = "")
    private synchronized Queue<String> ordering(InjectionPoint injectionPoint) {
        String id = getAnnotationObject(OrderId.class, injectionPoint).value();
        orderQueue.add(id);
        return orderQueue;
    }

    private <T> T getAnnotationObject(Class<T> annotationClass, InjectionPoint injectionPoint) {
         T annotationObject = null;
        for (Annotation qualifier : injectionPoint.getQualifiers()) {
            if (qualifier.annotationType() == annotationClass) {
                annotationObject = annotationClass.cast(qualifier);
            }
        }

        if (annotationObject == null) {
            throw new IllegalStateException("No counter id for injection point: " + injectionPoint.getMember());
        }

        return annotationObject;

    }
}
