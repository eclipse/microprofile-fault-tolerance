/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0S;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1S;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2S;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.exception.hierarchy.FallbackService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Test that {@code Fallback.applyOn()} and {@code Fallback.skipOn()} handle exception subclasses correctly.
 */
public class FallbackExceptionHierarchyTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftFallbackExceptionHierarchy.jar")
                .addPackage(E0.class.getPackage())
                .addClass(FallbackService.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "ftFallbackExceptionHierarchy.war")
                .addAsLibrary(jar);
    }

    @Inject
    private Instance<FallbackService> serviceInstance;

    private FallbackService service;

    @BeforeMethod
    public void setup() {
        if (serviceInstance != null) {
            service = serviceInstance.get();
        }
    }

    @AfterMethod
    public void teardown() {
        if (serviceInstance != null) {
            serviceInstance.destroy(service);
        }
    }

    // the <: symbol denotes the subtyping relation (Foo <: Bar means "Foo is a subtype of Bar")
    // note that subtyping is reflexive (Foo <: Foo)

    // E0 <: Exception
    // E1 <: E0
    // E2 <: E1
    // E2S <: E2
    // E1S <: E1, but not E1S <: E2
    // E0S <: E0, but not E0S <: E1

    // serviceA: @Fallback(applyOn = {E0.class, E2.class}, skipOn = E1.class, fallbackMethod = "myFallback")

    @Test
    public void serviceAthrowsException() {
        // serviceA doesn't mention Exception (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceA(new Exception()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceAthrowsE0() {
        // serviceA directly mentions E0 in applyOn
        assertEquals(invokeServiceA(new E0()), FallbackStatus.FALLBACK);
    }

    @Test
    public void serviceAthrowsE1() {
        // serviceA directly mentions E1 in skipOn
        assertEquals(invokeServiceA(new E1()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceAthrowsE2() {
        // serviceA directly mentions E2 in applyOn, but it also mentions E1 in skipOn and E2 <: E1
        assertEquals(invokeServiceA(new E2()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceAthrowsE2S() {
        // serviceA mentions E2 in applyOn and E2S <: E2, but it also mentions E1 in skipOn and E2S <: E1
        assertEquals(invokeServiceA(new E2S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceAthrowsE1S() {
        // serviceA mentions E1 in skipOn and E1S <: E1
        assertEquals(invokeServiceA(new E1S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceAthrowsE0S() {
        // serviceA mentions E0 in applyOn and E0S <: E0
        assertEquals(invokeServiceA(new E0S()), FallbackStatus.FALLBACK);
    }

    @Test
    public void serviceAthrowsRuntimeException() {
        // serviceA doesn't mention RuntimeException (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceA(new RuntimeException()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceAthrowsError() {
        // serviceA doesn't mention Error (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceA(new Error()), FallbackStatus.NOFALLBACK);
    }

    private FallbackStatus invokeServiceA(Throwable exception) {
        try {
            String result = service.serviceA(exception);
            if (service.getFallbackValue().equals(result)) {
                return FallbackStatus.FALLBACK;
            } else {
                return FallbackStatus.NOFALLBACK;
            }
        } catch (Throwable ex) {
            return FallbackStatus.NOFALLBACK;
        }
    }

    // serviceB: @Fallback(applyOn = {Exception.class, E1.class}, skipOn = {E0.class, E2.class}, fallbackMethod =
    // "myFallback")

    @Test
    public void serviceBthrowsException() {
        // serviceB directly mentions Exception in applyOn
        assertEquals(invokeserviceB(new Exception()), FallbackStatus.FALLBACK);
    }

    @Test
    public void serviceBthrowsE0() {
        // serviceB directly mentions E0 in skipOn
        assertEquals(invokeserviceB(new E0()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceBthrowsE1() {
        // serviceB directly mentions E1 in applyOn, but it also mentions E0 in skipOn and E1 <: E0
        assertEquals(invokeserviceB(new E1()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceBthrowsE2() {
        // serviceB directly mentions E2 in skipOn
        assertEquals(invokeserviceB(new E2()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceBthrowsE2S() {
        // serviceB mentions E2 in skipOn and E2S <: E2
        assertEquals(invokeserviceB(new E2S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceBthrowsE1S() {
        // serviceB mentions E1 in applyOn and E1S <: E1, but it also mentions E0 in skipOn and E1S <: E0
        assertEquals(invokeserviceB(new E1S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceBthrowsE0S() {
        // serviceB mentions E0 in skipOn and E0S <: E0
        assertEquals(invokeserviceB(new E0S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceBthrowsRuntimeException() {
        // serviceB mentions Exception in applyOn and RuntimeException <: Exception
        assertEquals(invokeserviceB(new RuntimeException()), FallbackStatus.FALLBACK);
    }

    @Test
    public void serviceBthrowsError() {
        // serviceB doesn't mention Error (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceA(new Error()), FallbackStatus.NOFALLBACK);
    }

    private FallbackStatus invokeserviceB(Throwable exception) {
        try {
            String result = service.serviceB(exception);
            if (service.getFallbackValue().equals(result)) {
                return FallbackStatus.FALLBACK;
            } else {
                return FallbackStatus.NOFALLBACK;
            }
        } catch (Throwable ex) {
            return FallbackStatus.NOFALLBACK;
        }
    }

    // serviceC: @Fallback(applyOn = {E1.class, E2.class}, skipOn = E0.class, fallbackMethod = "myFallback")

    @Test
    public void serviceCthrowsException() {
        // serviceC doesn't mention Exception (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceC(new Exception()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsE0() {
        // serviceC directly mentions E0 in skipOn
        assertEquals(invokeServiceC(new E0()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsE1() {
        // serviceC directly mentions E1 in applyOn, but it also mentions E0 in skipOn and E1 <: E0
        assertEquals(invokeServiceC(new E1()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsE2() {
        // serviceC directly mentions E2 in applyOn, but it also mentions E0 in skipOn and E1 <: E0
        assertEquals(invokeServiceC(new E2()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsE2S() {
        // serviceC mentions E2 in applyOn and E2S <: E2, but it also mentions E0 in skipOn and E2S <: E0
        assertEquals(invokeServiceC(new E2S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsE1S() {
        // serviceC mentions E1 in applyOn and E1S <: E1, but it also mentions E0 in skipOn and E1S <: E0
        assertEquals(invokeServiceC(new E1S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsE0S() {
        // serviceC mentions E0 in skipOn and E0S <: E0
        assertEquals(invokeServiceC(new E0S()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsRuntimeException() {
        // serviceC doesn't mention RuntimeException (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceC(new RuntimeException()), FallbackStatus.NOFALLBACK);
    }

    @Test
    public void serviceCthrowsError() {
        // serviceC doesn't mention Error (nor any of its superclasses) neither in applyOn nor skipOn
        assertEquals(invokeServiceC(new Error()), FallbackStatus.NOFALLBACK);
    }

    private FallbackStatus invokeServiceC(Throwable exception) {
        try {
            String result = service.serviceC(exception);
            if (service.getFallbackValue().equals(result)) {
                return FallbackStatus.FALLBACK;
            } else {
                return FallbackStatus.NOFALLBACK;
            }
        } catch (Throwable ex) {
            return FallbackStatus.NOFALLBACK;
        }
    }

    private enum FallbackStatus {
        FALLBACK, NOFALLBACK
    }
}
