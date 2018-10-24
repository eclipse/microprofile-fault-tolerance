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
package org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver;

import static org.testng.Assert.fail;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

@RequestScoped
public class CircuitBreakerClientWithTimeout {

    /**
     * Sleeps for 1000ms, times out after 500ms
     * <p>
     * CircuitBreaker opens after two failed requests
     * 
     * @return should always throw TimeoutException, unless CircuitBreaker prevents execution
     */
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 2, failureRatio = 0.75, delay = 50000)
    @Timeout(500)
    public String serviceWithTimeout() {
        try {
            Thread.sleep(1000);
            fail("Thread not interrupted by timeout");
        }
        catch (InterruptedException e) {
            // Expected
        }
        return "OK";
    }
    
    /**
     * Sleeps for 1000ms, times out after 500ms
     * <p>
     * CircuitBreaker opens after two BulkheadExceptions
     * <p>
     * The method should never throw a BulkheadException so the CircuitBreaker should have no effect
     * 
     * @return should always throw TimeoutException
     */
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 2, failureRatio = 0.75, delay = 50000, failOn = BulkheadException.class)
    @Timeout(500)
    public String serviceWithTimeoutWithoutFailOn() {
        try {
            Thread.sleep(1000);
            fail("Thread not interrupted by timeout");
        }
        catch (InterruptedException e) {
            // Expected
        }
        return "OK";
    }
}
