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
package org.eclipse.microprofile.fault.tolerance.tck.disableEnv;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.util.ConcurrentExecutionTracker;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * A client to determine the impact of disabling annotations via config
 * <p>
 * Each method has an easy test to determine whether it's annotations are active or not.
 * 
 * @author <a href="mailto:anrouse@uk.ibm.com">Andrew Rouse</a>
 *
 */
@ApplicationScoped
public class DisableAnnotationClient {

    private int failAndRetryOnceCounter = 0;
    private int failRetryOnceThenFallbackCounter = 0;
    
    @Inject private ConcurrentExecutionTracker tracker;

    /**
     * Always throws {@link TestException}, should increment counter by two if Retry is enabled, or one if it is not
     */
    @Retry(maxRetries = 1)
    public void failAndRetryOnce() {
        failAndRetryOnceCounter++;
        throw new TestException();
    }
    
    /**
     * Returns the number of times that {@link #failAndRetryOnce()} has been executed
     *
     * @return failAndRetryOnceCounter
     */
    public int getFailAndRetryOnceCounter() {
        return failAndRetryOnceCounter;
    }
    
    /**
     * Should return normally if Fallback is enabled or throw TestException if not
     * <p>
     * Should increment counter by two if Retry is enabled or one if it is not
     * 
     * @return nothing, always throws TestException
     */
    @Retry(maxRetries = 1)
    @Fallback(fallbackMethod = "fallback")
    public String failRetryOnceThenFallback() {
        failRetryOnceThenFallbackCounter++;
        throw new TestException();
    }
    
    /**
     * Returns the number of times that {@link #failRetryOnceThenFallback()} has been executed
     *
     * @return failRetryOnceThenFallbackCounter
     */
    public int getFailRetryOnceThenFallbackCounter() {
        return failRetryOnceThenFallbackCounter;
    }
    
    public String fallback() {
        return "OK";
    }
    
    /**
     * Always throws TestException on first invocation, throws {@link org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException}
     * on second if CircuitBreaker is enabled
     * <p>
     * Throw test exception on second invocation if CircuitBreaker is disabled
     */
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 1, failureRatio = 1, delay = 50000)
    public void failWithCircuitBreaker() {
        throw new TestException();
    }
    
    /**
     * Throws {@link org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException} if Timeout is enabled or TestException otherwise
     */
    @Timeout(500)
    public void failWithTimeout() {
        try {
            Thread.sleep(2000);
            throw new TestException();
        }
        catch (InterruptedException e) {
            //expected
        }
    }
    
    /**
     * Blocks waiting for {@code waitingFuture} to complete
     * <p>
     * If passed an already completed {@link Future}, this method will return immediately.
     * <p>
     * Should permit two simultaneous calls if bulkhead enabled, or more if bulkhead disabled.
     * 
     * @param waitingFuture the future to wait for
     */
    @Bulkhead(2)
    public void waitWithBulkhead(Future<?> waitingFuture) {
        try {
            tracker.executionStarted();
            waitingFuture.get();
        }
        catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        finally {
            tracker.executionEnded();
        }
    }
    
    /**
     * Wait for {@code count} executions of {@link #waitWithBulkhead(Future)} to be in progress.
     *
     * @param count execution count
     */
    public void waitForBulkheadExecutions(int count) {
        tracker.waitForRunningExecutions(count);
    }
    
    /**
     * Returns a future which will be complete on method return if Asynchronous is disabled,
     * or incomplete if Asynchronous is enabled.
     *
     * @return Completed future
     */
    @Asynchronous
    public Future<String> asyncWaitThenReturn() {
        try {
            Thread.sleep(2000);
            return CompletableFuture.completedFuture("OK");
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
