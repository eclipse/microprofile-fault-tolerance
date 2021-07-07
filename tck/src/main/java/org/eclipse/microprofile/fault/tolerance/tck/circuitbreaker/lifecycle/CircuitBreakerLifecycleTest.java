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
package org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.lifecycle;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Test that circuit breaker is a singleton, even if the bean is not.
 */
public class CircuitBreakerLifecycleTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftCircuitBreakerLifecycle.jar")
                .addPackage(CircuitBreakerLifecycleService.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "ftCircuitBreakerLifecycle.war")
                .addAsLibrary(testJar);
    }

    @Inject
    private Instance<Object> beans;

    // verify that circuit breaker is shared between instances of the same class and for the same method,
    // in various inheritance hierarchy scenarios

    @Test
    public void circuitBreakerOnClass() {
        invokeService(BaseCircuitBreakerOnClass.class, 8);
    }

    @Test
    public void circuitBreakerOnClassOverrideOnClass() {
        invokeService(DerivedCircuitBreakerOnClassOverrideOnClass.class, 4);
    }

    @Test
    public void circuitBreakerOnClassOverrideOnClassWithOverriddenMethod() {
        invokeService(DerivedCircuitBreakerOnClassOverrideOnClassWithOverriddenMethod.class, 4);
    }

    @Test
    public void circuitBreakerOnClassOverrideOnMethod() {
        invokeService(DerivedCircuitBreakerOnClassOverrideOnMethod.class, 4);
    }

    @Test
    public void circuitBreakerOnClassMissingOnOverriddenMethod() {
        invokeService(DerivedCircuitBreakerOnClassNoAnnotationOnOverriddenMethod.class, 8);
    }

    @Test
    public void circuitBreakerOnClassNoRedefinition() {
        invokeService(DerivedCircuitBreakerOnClassNoRedefinition.class, 8);
    }

    @Test
    public void circuitBreakerOnMethod() {
        invokeService(BaseCircuitBreakerOnMethod.class, 8);
    }

    @Test
    public void circuitBreakerOnMethodOverrideOnClass() {
        // CB on inherited method takes precedence over CB on derived class
        invokeService(DerivedCircuitBreakerOnMethodOverrideOnClass.class, 8);
    }

    @Test
    public void circuitBreakerOnMethodOverrideOnClassWithOverriddenMethod() {
        invokeService(DerivedCircuitBreakerOnMethodOverrideOnClassWithOverriddenMethod.class, 4);
    }

    @Test
    public void circuitBreakerOnMethodOverrideOnMethod() {
        invokeService(DerivedCircuitBreakerOnMethodOverrideOnMethod.class, 4);
    }

    @Test
    public void circuitBreakerOnMethodMissingOnOverriddenMethod() {
        invokeService(DerivedCircuitBreakerOnMethodNoAnnotationOnOverriddenMethod.class, 100);
    }

    @Test
    public void circuitBreakerOnMethodNoRedefinition() {
        invokeService(DerivedCircuitBreakerOnMethodNoRedefinition.class, 8);
    }

    @Test
    public void circuitBreakerOnClassAndMethod() {
        invokeService(BaseCircuitBreakerOnClassAndMethod.class, 8);
    }

    @Test
    public void circuitBreakerOnClassAndMethodOverrideOnClass() {
        // CB on inherited method takes precedence over CB on derived class
        invokeService(DerivedCircuitBreakerOnClassAndMethodOverrideOnClass.class, 8);
    }

    @Test
    public void circuitBreakerOnClassAndMethodOverrideOnClassWithOverriddenMethod() {
        invokeService(DerivedCircuitBreakerOnClassAndMethodOverrideOnClassWithOverriddenMethod.class, 4);
    }

    @Test
    public void circuitBreakerOnClassAndMethodOverrideOnMethod() {
        invokeService(DerivedCircuitBreakerOnClassAndMethodOverrideOnMethod.class, 4);
    }

    @Test
    public void circuitBreakerOnClassAndMethodMissingOnOverriddenMethod() {
        // missing on overridden method, so inherited from base class
        invokeService(DerivedCircuitBreakerOnClassAndMethodNoAnnotationOnOverriddenMethod.class, 16);
    }

    @Test
    public void circuitBreakerOnClassAndMethodNoRedefinition() {
        invokeService(DerivedCircuitBreakerOnClassAndMethodNoRedefinition.class, 8);
    }

    private <T extends CircuitBreakerLifecycleService> void invokeService(Class<T> clazz,
            int expectedCallsNotPrevented) {
        Instance<T> serviceProvider = beans.select(clazz);
        int servicesCount = expectedCallsNotPrevented / 2;

        List<T> services = new ArrayList<>(servicesCount);
        for (int i = 0; i < servicesCount; i++) {
            services.add(serviceProvider.get());
        }

        try {
            Set<Integer> instanceIds = new HashSet<>();
            for (int i = 0; i < servicesCount; i++) {
                instanceIds.add(services.get(i).instanceId());
            }
            assertEquals(instanceIds.size(), servicesCount);

            int serviceCounter = 0;
            for (int i = 0; i < expectedCallsNotPrevented; i++) {
                CircuitBreakerLifecycleService service = services.get(serviceCounter);

                assertThrows(IOException.class, service::service);

                serviceCounter = (serviceCounter + 1) % servicesCount;
            }

            // expectedCallsNotPrevented >= 100 means that _all_ calls are expected to be not prevented;
            // in other words, the method is not guarded by a circuit breaker
            Class<? extends Throwable> expectedException = expectedCallsNotPrevented < 100
                    ? CircuitBreakerOpenException.class
                    : IOException.class;
            for (CircuitBreakerLifecycleService service : services) {
                assertThrows(expectedException, service::service);
            }
        } finally {
            for (T service : services) {
                serviceProvider.destroy(service);
            }
        }
    }

    // ---
    // verify that circuit breaker is shared between instances of the same class and for the same method,
    // but not shared between different classes and different methods of the same class

    @Inject
    private Instance<CircuitBreakerLifecycleService1> service1;

    @Inject
    private Instance<CircuitBreakerLifecycleService2> service2;

    @Inject
    private Instance<MutlipleMethodsCircuitBreakerLifecycleService> multipleMethodsService;

    @Test
    public void noSharingBetweenClasses() {
        CircuitBreakerLifecycleService1 service1a = service1.get();
        CircuitBreakerLifecycleService1 service1b = service1.get();

        CircuitBreakerLifecycleService2 service2a = service2.get();
        CircuitBreakerLifecycleService2 service2b = service2.get();

        try {
            for (int i = 0; i < 4; i++) {
                assertThrows(IOException.class, service1a::service);
                assertThrows(IOException.class, service2a::service);
                assertThrows(IOException.class, service1b::service);
                assertThrows(IOException.class, service2b::service);
            }

            assertThrows(CircuitBreakerOpenException.class, service1a::service);
            assertThrows(CircuitBreakerOpenException.class, service2a::service);
            assertThrows(CircuitBreakerOpenException.class, service1b::service);
            assertThrows(CircuitBreakerOpenException.class, service2b::service);
        } finally {
            service1.destroy(service1a);
            service1.destroy(service1b);
            service2.destroy(service2a);
            service2.destroy(service2b);
        }
    }

    @Test
    public void noSharingBetweenMethodsOfOneClass() {
        MutlipleMethodsCircuitBreakerLifecycleService multipleMethodsService1 = multipleMethodsService.get();
        MutlipleMethodsCircuitBreakerLifecycleService multipleMethodsService2 = multipleMethodsService.get();

        try {
            for (int i = 0; i < 4; i++) {
                assertThrows(IOException.class, multipleMethodsService1::service1);
                assertThrows(IOException.class, multipleMethodsService1::service2);
                assertThrows(IOException.class, multipleMethodsService2::service1);
                assertThrows(IOException.class, multipleMethodsService2::service2);
            }

            assertThrows(CircuitBreakerOpenException.class, multipleMethodsService1::service1);
            assertThrows(CircuitBreakerOpenException.class, multipleMethodsService1::service2);
            assertThrows(CircuitBreakerOpenException.class, multipleMethodsService2::service1);
            assertThrows(CircuitBreakerOpenException.class, multipleMethodsService2::service2);
        } finally {
            multipleMethodsService.destroy(multipleMethodsService1);
            multipleMethodsService.destroy(multipleMethodsService2);
        }
    }
}
