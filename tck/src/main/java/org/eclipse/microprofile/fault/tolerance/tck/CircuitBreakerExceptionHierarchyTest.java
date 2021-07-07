/*
 *******************************************************************************
 * Copyright (c) 2019, 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.fault.tolerance.tck;

import static org.testng.Assert.assertEquals;

import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.exception.hierarchy.CircuitBreakerService;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0S;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1S;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2S;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that {@code CircuitBreaker.failOn()} and {@code CircuitBreaker.skipOn()} handle exception subclasses correctly.
 */
public class CircuitBreakerExceptionHierarchyTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreakerExceptionHierarchy.jar")
                .addPackage(E0.class.getPackage())
                .addClass(CircuitBreakerService.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "ftCircuitBreakerExceptionHierarchy.war")
                .addAsLibrary(jar);
    }

    @Inject
    private CircuitBreakerService service;

    // the <: symbol denotes the subtyping relation (Foo <: Bar means "Foo is a subtype of Bar")
    // note that subtyping is reflexive (Foo <: Foo)

    // E0 <: Exception
    // E1 <: E0
    // E2 <: E1
    // E2S <: E2
    // E1S <: E1, but not E1S <: E2
    // E0S <: E0, but not E0S <: E1

    // serviceA: @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class)

    @Test
    public void serviceAthrowsException() {
        // serviceA doesn't mention Exception (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceA1, new Exception()), CircuitState.CLOSED);
    }

    @Test
    public void serviceAthrowsE0() {
        // serviceA directly mentions E0 in failOn
        assertEquals(invoke(service::serviceA2, new E0()), CircuitState.OPEN);
    }

    @Test
    public void serviceAthrowsE1() {
        // serviceA directly mentions E1 in skipOn
        assertEquals(invoke(service::serviceA3, new E1()), CircuitState.CLOSED);
    }

    @Test
    public void serviceAthrowsE2() {
        // serviceA directly mentions E2 in failOn, but it also mentions E1 in skipOn and E2 <: E1
        assertEquals(invoke(service::serviceA4, new E2()), CircuitState.CLOSED);
    }

    @Test
    public void serviceAthrowsE2S() {
        // serviceA mentions E2 in failOn and E2S <: E2, but it also mentions E1 in skipOn and E2S <: E1
        assertEquals(invoke(service::serviceA5, new E2S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceAthrowsE1S() {
        // serviceA mentions E1 in skipOn and E1S <: E1
        assertEquals(invoke(service::serviceA6, new E1S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceAthrowsE0S() {
        // serviceA mentions E0 in failOn and E0S <: E0
        assertEquals(invoke(service::serviceA7, new E0S()), CircuitState.OPEN);
    }

    @Test
    public void serviceAthrowsRuntimeException() {
        // serviceA doesn't mention RuntimeException (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceA8, new RuntimeException()), CircuitState.CLOSED);
    }

    @Test
    public void serviceAthrowsError() {
        // serviceA doesn't mention Error (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceA9, new Error()), CircuitState.CLOSED);
    }

    // serviceB: @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class, E2.class})

    @Test
    public void serviceBthrowsException() {
        // serviceB directly mentions Exception in failOn
        assertEquals(invoke(service::serviceB1, new Exception()), CircuitState.OPEN);
    }

    @Test
    public void serviceBthrowsE0() {
        // serviceB directly mentions E0 in skipOn
        assertEquals(invoke(service::serviceB2, new E0()), CircuitState.CLOSED);
    }

    @Test
    public void serviceBthrowsE1() {
        // serviceB directly mentions E1 in failOn, but it also mentions E0 in skipOn and E1 <: E0
        assertEquals(invoke(service::serviceB3, new E1()), CircuitState.CLOSED);
    }

    @Test
    public void serviceBthrowsE2() {
        // serviceB directly mentions E2 in skipOn
        assertEquals(invoke(service::serviceB4, new E2()), CircuitState.CLOSED);
    }

    @Test
    public void serviceBthrowsE2S() {
        // serviceB mentions E2 in skipOn and E2S <: E2
        assertEquals(invoke(service::serviceB5, new E2S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceBthrowsE1S() {
        // serviceB mentions E1 in failOn and E1S <: E1, but it also mentions E0 in skipOn and E1S <: E0
        assertEquals(invoke(service::serviceB6, new E1S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceBthrowsE0S() {
        // serviceB mentions E0 in skipOn and E0S <: E0
        assertEquals(invoke(service::serviceB7, new E0S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceBthrowsRuntimeException() {
        // serviceB mentions Exception in failOn and RuntimeException <: Exception
        assertEquals(invoke(service::serviceB8, new RuntimeException()), CircuitState.OPEN);
    }

    @Test
    public void serviceBthrowsError() {
        // serviceB doesn't mention Error (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceB9, new Error()), CircuitState.CLOSED);
    }

    // serviceC: @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class)

    @Test
    public void serviceCthrowsException() {
        // serviceC doesn't mention Exception (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceC1, new Exception()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsE0() {
        // serviceC directly mentions E0 in skipOn
        assertEquals(invoke(service::serviceC2, new E0()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsE1() {
        // serviceC directly mentions E1 in failOn, but it also mentions E0 in skipOn and E1 <: E0
        assertEquals(invoke(service::serviceC3, new E1()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsE2() {
        // serviceC directly mentions E2 in failOn, but it also mentions E0 in skipOn and E1 <: E0
        assertEquals(invoke(service::serviceC4, new E2()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsE2S() {
        // serviceC mentions E2 in failOn and E2S <: E2, but it also mentions E0 in skipOn and E2S <: E0
        assertEquals(invoke(service::serviceC5, new E2S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsE1S() {
        // serviceC mentions E1 in failOn and E1S <: E1, but it also mentions E0 in skipOn and E1S <: E0
        assertEquals(invoke(service::serviceC6, new E1S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsE0S() {
        // serviceC mentions E0 in skipOn and E0S <: E0
        assertEquals(invoke(service::serviceC7, new E0S()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsRuntimeException() {
        // serviceC doesn't mention RuntimeException (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceC8, new RuntimeException()), CircuitState.CLOSED);
    }

    @Test
    public void serviceCthrowsError() {
        // serviceC doesn't mention Error (nor any of its superclasses) neither in failOn nor skipOn
        assertEquals(invoke(service::serviceC9, new Error()), CircuitState.CLOSED);
    }

    private CircuitState invoke(ServiceMethod method, Throwable exception) {
        try {
            method.invoke(exception);
            throw new AssertionError("Exception not thrown from service method");
        } catch (Throwable ex) {
            assertEquals(ex, exception);
        }

        try {
            method.invoke(exception);
            throw new AssertionError("Exception not thrown from service method");
        } catch (CircuitBreakerOpenException ex) {
            return CircuitState.OPEN;
        } catch (Throwable ex) {
            assertEquals(ex, exception);
            return CircuitState.CLOSED;
        }
    }

    @FunctionalInterface
    private static interface ServiceMethod {
        void invoke(Throwable t) throws Throwable;
    }

    private enum CircuitState {
        CLOSED, OPEN
    }
}
