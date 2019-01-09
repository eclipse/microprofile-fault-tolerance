/*
 *******************************************************************************
 * Copyright (c) 2016-2019 Contributors to the Eclipse Foundation
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

import java.io.Serializable;
import java.sql.Connection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

/**
 * A client to exercise Circuit Breaker thresholds using Retries.
 * 
 * Analogous to {@link CircuitBreakerClientWithRetry} but methods are annotated
 * with {@link Asynchronous}
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 * @author <a href="mailto:anrouse@uk.ibm.com">Andrew Rouse</a>
 */
@RequestScoped
public class CircuitBreakerClientWithRetryAsync implements Serializable {
    private int counterForInvokingServiceA = 0;
    private int counterForInvokingServiceB = 0;
    private int counterForInvokingServiceC = 0;

    public int getCounterForInvokingServiceA() {
        return counterForInvokingServiceA;
    }
    
    public int getCounterForInvokingServiceB() {
        return counterForInvokingServiceB;
    }

    public int getCounterForInvokingServiceC() {
        return counterForInvokingServiceC;
    }
    
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
    @Retry(retryOn = {TestException.class}, maxRetries = 7)
    @Asynchronous
    public Future<Connection> serviceA() {
        Connection conn = null;
        counterForInvokingServiceA++;
        conn = connectionService();
        return CompletableFuture.completedFuture(conn);
    }

    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
    @Retry(retryOn = {TestException.class}, maxRetries = 2)
    @Asynchronous
    public Future<Connection> serviceB() {
        Connection conn = null;
        counterForInvokingServiceB++;
        conn = connectionService();
        return CompletableFuture.completedFuture(conn);
    }
    
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
    @Retry(retryOn = {TestException.class, TimeoutException.class}, maxRetries = 7, maxDuration = 20000)
    @Timeout(500)
    @Asynchronous
    public Future<Connection> serviceC(long timeToSleep) {
        Connection conn = null;
        counterForInvokingServiceC++;

        try {
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return CompletableFuture.completedFuture(conn);
    }
    
    /**
     * Has a CircuitBreaker and Retries on CircuitBreakerOpenException
     * 
     * @param throwException whether this method should throw a runtime exception to simulate an application failure
     * @return string "OK"
     */
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75, delay = 1000)
    @Retry(retryOn = CircuitBreakerOpenException.class, maxRetries = 20, delay = 100, jitter = 0)
    @Asynchronous
    public Future<String> serviceWithRetryOnCbOpen(boolean throwException) {
        if (throwException) {
            throw new TestException("Test Exception");
        }
        else {
            return CompletableFuture.completedFuture("OK");
        }
    }
    
    /**
     * Has a CircuitBreaker and Retries on TimeoutException
     * <p>
     * The method should never throw a TimeoutException so the retry should have no effect
     * 
     * @param throwException whether this method should throw a TestException to simulate an application failure
     * @return string "OK"
     */
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75, delay = 1000)
    @Retry(retryOn = TimeoutException.class, maxRetries = 20, delay = 200)
    @Asynchronous
    public Future<String> serviceWithRetryOnTimeout(boolean throwException) {
        if (throwException) {
            throw new TestException("Test Exception");
        }
        else {
            return CompletableFuture.completedFuture("OK");
        }
    }
    
    /**
     * Has a CircuitBreaker and Retries on all exceptions except TestException and CircuitBreakerOpenException
     * 
     * @param throwException whether this method should throw a TestException to simulate an application failure
     * @return string "OK"
     */
    @CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.75, delay = 1000)
    @Retry(abortOn = { TestException.class, CircuitBreakerOpenException.class }, maxRetries = 20, delay = 200)
    @Asynchronous
    public Future<String> serviceWithRetryFailOnCbOpen(boolean throwException) {
        if (throwException) {
            throw new TestException("Test Exception");
        }
        else {
            return CompletableFuture.completedFuture("OK");
        }
    }
    
    //simulate a backend service
    private Connection connectionService() {
        throw new TestException("Connection failed");
    }
}