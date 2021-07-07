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

package org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.exception.hierarchy;

import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Contains three circuit breaker configurations for testing the interaction between failOn and skipOn
 * <p>
 * Each test service is replicated several times so that each test uses a separate circuit breaker
 */
@ApplicationScoped
public class CircuitBreakerService {

    /*
     * -------------------- serviceA -------------------- failOn = E0, E2 skipOn = E1
     */

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA1(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA2(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA3(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA4(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA5(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA6(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA7(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA8(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E0.class, E2.class}, skipOn = E1.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceA9(Throwable exception) throws Throwable {
        throw exception;
    }

    /*
     * -------------------- serviceB -------------------- failOn = Exception, E1 skipOn = E0, E2
     */

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB1(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB2(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB3(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB4(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB5(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB6(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB7(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB8(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {Exception.class, E1.class}, skipOn = {E0.class,
            E2.class}, requestVolumeThreshold = 1, delay = 20000)
    public void serviceB9(Throwable exception) throws Throwable {
        throw exception;
    }

    /*
     * -------------------- serviceC -------------------- failOn = E1, E2 skipOn = E0
     */

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC1(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC2(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC3(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC4(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC5(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC6(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC7(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC8(Throwable exception) throws Throwable {
        throw exception;
    }

    @CircuitBreaker(failOn = {E1.class, E2.class}, skipOn = E0.class, requestVolumeThreshold = 1, delay = 20000)
    public void serviceC9(Throwable exception) throws Throwable {
        throw exception;
    }

}
