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
import org.eclipse.microprofile.fault.tolerance.tck.retry.exception.hierarchy.RetryService;
import org.eclipse.microprofile.fault.tolerance.tck.retry.exception.hierarchy.RetryStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Verifies behavior of {@code @Retry} for various exceptions in an inheritance hierarchy. The asserted logic is:
 * <ol>
 * <li>Abort if the exception matches abortOn</li>
 * <li>Otherwise retry if the exception matches retryOn</li>
 * <li>Otherwise abort</li>
 * </ol>
 */
public class RetryExceptionHierarchyTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftRetryExceptionHierarchy.jar")
                .addClasses(E0.class, E0S.class, E1.class, E1S.class, E2.class, E2S.class, RetryStatus.class,
                        RetryService.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap
                .create(WebArchive.class, "ftRetryExceptionHierarchy.war")
                .addAsLibrary(jar);
    }

    @Inject
    private RetryService service;

    // the <: symbol denotes the subtyping relation (Foo <: Bar means "Foo is a subtype of Bar")
    // note that subtyping is reflexive (Foo <: Foo)

    // E0 <: Exception
    // E1 <: E0
    // E2 <: E1
    // E2S <: E2
    // E1S <: E1, but not E1S <: E2
    // E0S <: E0, but not E0S <: E1

    // serviceA: @Retry(retryOn = {E0.class, E2.class}, abortOn = E1.class)

    @Test
    public void serviceAthrowsException() {
        // serviceA doesn't mention Exception (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceA(new Exception()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceAthrowsE0() {
        // serviceA directly mentions E0 in retryOn
        assertEquals(invokeServiceA(new E0()), RetryStatus.RETRIED_INVOCATION);
    }

    @Test
    public void serviceAthrowsE1() {
        // serviceA directly mentions E1 in abortOn
        assertEquals(invokeServiceA(new E1()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceAthrowsE2() {
        // serviceA directly mentions E2 in retryOn, but it also mentions E1 in abortOn and E2 <: E1
        assertEquals(invokeServiceA(new E2()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceAthrowsE2S() {
        // serviceA mentions E2 in retryOn and E2S <: E2, but it also mentions E1 in abortOn and E2S <: E1
        assertEquals(invokeServiceA(new E2S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceAthrowsE1S() {
        // serviceA mentions E1 in abortOn and E1S <: E1
        assertEquals(invokeServiceA(new E1S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceAthrowsE0S() {
        // serviceA mentions E0 in retryOn and E0S <: E0
        assertEquals(invokeServiceA(new E0S()), RetryStatus.RETRIED_INVOCATION);
    }

    @Test
    public void serviceAthrowsRuntimeException() {
        // serviceA doesn't mention RuntimeException (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceA(new RuntimeException()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceAthrowsError() {
        // serviceA doesn't mention Error (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceA(new Error()), RetryStatus.FIRST_INVOCATION);
    }

    private RetryStatus invokeServiceA(Throwable exception) {
        try {
            service.serviceA(exception);
        } catch (Throwable expected) {
        }
        return service.getStatus();
    }

    // serviceB: @Retry(retryOn = {Exception.class, E1.class}, abortOn = {E0.class, E2.class})

    @Test
    public void serviceBthrowsException() {
        // serviceB directly mentions Exception in retryOn
        assertEquals(invokeserviceB(new Exception()), RetryStatus.RETRIED_INVOCATION);
    }

    @Test
    public void serviceBthrowsE0() {
        // serviceB directly mentions E0 in abortOn
        assertEquals(invokeserviceB(new E0()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceBthrowsE1() {
        // serviceB directly mentions E1 in retryOn, but it also mentions E0 in abortOn and E1 <: E0
        assertEquals(invokeserviceB(new E1()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceBthrowsE2() {
        // serviceB directly mentions E2 in abortOn
        assertEquals(invokeserviceB(new E2()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceBthrowsE2S() {
        // serviceB mentions E2 in abortOn and E2S <: E2
        assertEquals(invokeserviceB(new E2S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceBthrowsE1S() {
        // serviceB mentions E1 in retryOn and E1S <: E1, but it also mentions E0 in abortOn and E1S <: E0
        assertEquals(invokeserviceB(new E1S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceBthrowsE0S() {
        // serviceB mentions E0 in abortOn and E0S <: E0
        assertEquals(invokeserviceB(new E0S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceBthrowsRuntimeException() {
        // serviceB mentions Exception in retryOn and RuntimeException <: Exception
        assertEquals(invokeserviceB(new RuntimeException()), RetryStatus.RETRIED_INVOCATION);
    }

    @Test
    public void serviceBthrowsError() {
        // serviceB doesn't mention Error (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceA(new Error()), RetryStatus.FIRST_INVOCATION);
    }

    private RetryStatus invokeserviceB(Throwable exception) {
        try {
            service.serviceB(exception);
        } catch (Throwable expected) {
        }
        return service.getStatus();
    }

    // serviceC: @Retry(retryOn = {E1.class, E2.class}, abortOn = E0.class)

    @Test
    public void serviceCthrowsException() {
        // serviceC doesn't mention Exception (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceC(new Exception()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsE0() {
        // serviceC directly mentions E0 in abortOn
        assertEquals(invokeServiceC(new E0()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsE1() {
        // serviceC directly mentions E1 in retryOn, but it also mentions E0 in abortOn and E1 <: E0
        assertEquals(invokeServiceC(new E1()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsE2() {
        // serviceC directly mentions E2 in retryOn, but it also mentions E0 in abortOn and E1 <: E0
        assertEquals(invokeServiceC(new E2()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsE2S() {
        // serviceC mentions E2 in retryOn and E2S <: E2, but it also mentions E0 in abortOn and E2S <: E0
        assertEquals(invokeServiceC(new E2S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsE1S() {
        // serviceC mentions E1 in retryOn and E1S <: E1, but it also mentions E0 in abortOn and E1S <: E0
        assertEquals(invokeServiceC(new E1S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsE0S() {
        // serviceC mentions E0 in abortOn and E0S <: E0
        assertEquals(invokeServiceC(new E0S()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsRuntimeException() {
        // serviceC doesn't mention RuntimeException (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceC(new RuntimeException()), RetryStatus.FIRST_INVOCATION);
    }

    @Test
    public void serviceCthrowsError() {
        // serviceC doesn't mention Error (nor any of its superclasses) neither in retryOn nor abortOn
        assertEquals(invokeServiceC(new Error()), RetryStatus.FIRST_INVOCATION);
    }

    private RetryStatus invokeServiceC(Throwable exception) {
        try {
            service.serviceC(exception);
        } catch (Throwable expected) {
        }
        return service.getStatus();
    }
}
