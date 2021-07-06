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

package org.eclipse.microprofile.fault.tolerance.tck.config;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import jakarta.enterprise.context.Dependent;

/**
 * Suite of methods for testing the various parameters of CircuitBreaker
 */
@Dependent
public class CircuitBreakerConfigBean {

    /**
     * Method throws TestConfigExceptionA which will NOT result in CircuitBreakerOpenException on the third call, unless
     * failOn is configured to TestConfigExceptionA.
     */
    @CircuitBreaker(requestVolumeThreshold = 2, failOn = TestConfigExceptionB.class)
    public void failOnMethod() {
        throw new TestConfigExceptionA();
    }

    /**
     * Method throws TestConfigExceptionA which will result in CircuitBreakerOpenException on the third call, unless
     * skipOn is configured to TestConfigExceptionA.
     */
    @CircuitBreaker(requestVolumeThreshold = 2)
    public void skipOnMethod() {
        throw new TestConfigExceptionA();
    }

    /**
     * This method's circuit breaker moves from open to half-open after 10 micros, unless delay and delayUnit are
     * configured differently.
     */
    @CircuitBreaker(requestVolumeThreshold = 2, delay = 20, delayUnit = ChronoUnit.MICROS)
    public void delayMethod(boolean fail) {
        if (fail) {
            throw new TestConfigExceptionA();
        }
    }

    /**
     * Method throws TestConfigExceptionA which will result in CircuitBreakerOpenException on the third call, unless
     * requestVolumeThreshold is configured to a greater number.
     */
    @CircuitBreaker(requestVolumeThreshold = 2)
    public void requestVolumeThresholdMethod() {
        throw new TestConfigExceptionA();
    }

    /**
     * This method's circuit breaker moves from closed to open after 10 consecutive failures, unless failureRatio is
     * configured differently.
     */
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 1.0)
    public void failureRatioMethod(boolean fail) {
        if (fail) {
            throw new TestConfigExceptionA();
        }
    }

    /**
     * This method's circuit breaker moves from half-open to closed after 4 consecutive successes, unless
     * successThreshold is configured differently.
     */
    @CircuitBreaker(requestVolumeThreshold = 10, successThreshold = 4, delay = 1000)
    public void successThresholdMethod(boolean fail) {
        if (fail) {
            throw new TestConfigExceptionA();
        }
    }

}
