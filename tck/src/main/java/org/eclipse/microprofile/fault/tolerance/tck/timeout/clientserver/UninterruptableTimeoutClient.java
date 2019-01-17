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
package org.eclipse.microprofile.fault.tolerance.tck.timeout.clientserver;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.testng.Assert;

@RequestScoped
public class UninterruptableTimeoutClient {
    
    /**
     * Waits for at least {@code waitms}, then returns
     * <p>
     * Times out in 500ms.
     * <p>
     * Does not respect thread interruption.
     * <p>
     * Uses a tight loop so the thread interrupted flag should be set when the method returns
     * 
     * @param waitMs the time to wait
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    public void serviceTimeout(int waitMs) {
        long waitNs = Duration.ofMillis(waitMs).toNanos();
        long startTime = System.nanoTime();
        
        while (true) {
            if (System.nanoTime() - startTime > waitNs) {
                return;
            }
        }
    }
    
    /**
     * Waits for waitingFuture to complete, then returns.
     * <p>
     * Times out in 500ms.
     * <p>
     * Runs asynchronously.
     * <p>
     * Does not respect thread interruption.
     * 
     * @param waitingFuture future to wait for
     * @param completion future that this method will complete before returning
     * @return a completed future
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Asynchronous
    public Future<Void> serviceTimeoutAsync(Future<?> waitingFuture, CompletableFuture<Void> completion) {
        while (true) {
            try {
                waitingFuture.get(5, SECONDS);
                completion.complete(null);
                return CompletableFuture.completedFuture(null);
            }
            catch (InterruptedException e) {
                // Ignore
            }
            catch (ExecutionException e) {
                Assert.fail("Waiting future threw exception", e);
            }
            catch (TimeoutException e) {
                return CompletableFuture.completedFuture(null);
            }
        }
    }
    
    /**
     * Waits for at least {@code waitMs}, then returns
     * <p>
     * Times out in 500ms
     * <p>
     * Runs asynchronously
     * <p>
     * Does not respect thread interruption.
     * <p>
     * Uses a tight loop so the thread interrupted flag should be set when the method returns
     * 
     * @param waitMs the time to wait
     * @return a completed CompletionStage
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Asynchronous
    public CompletionStage<Void> serviceTimeoutAsyncCS(long waitMs) {
        long waitNs = Duration.ofMillis(waitMs).toNanos();
        long startTime = System.nanoTime();
        
        while (true) {
            if (System.nanoTime() - startTime > waitNs) {
                return CompletableFuture.completedFuture(null);
            }
        }
    }
    
    
    /**
     * Waits for waitingFuture to complete, then returns.
     * <p>
     * Times out in 500ms.
     * <p>
     * Runs asynchronously.
     * <p>
     * Does not respect thread interruption.
     * <p>
     * Has a bulkhead with capacity of 1, queue size of 1.
     * <p>
     * Increments timeoutAsyncBulkheadCounter.
     * 
     * @param waitingFuture future to wait for
     * @return a completed future
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    public Future<Void> serviceTimeoutAsyncBulkhead(Future<?> waitingFuture) {
        timeoutAsyncBulkheadCounter.incrementAndGet();
        while (true) {
            try {
                waitingFuture.get(5, SECONDS);
                return CompletableFuture.completedFuture(null);
            }
            catch (InterruptedException e) {
                // Ignore
            }
            catch (ExecutionException e) {
                Assert.fail("Waiting future threw exception", e);
            }
            catch (TimeoutException e) {
                return CompletableFuture.completedFuture(null);
            }
        }
    }
    
    private AtomicInteger timeoutAsyncBulkheadCounter = new AtomicInteger();
    
    public int getTimeoutAsyncBulkheadCounter() {
        return timeoutAsyncBulkheadCounter.get();
    }
    
    /**
     * Waits for waitingFuture to complete, then returns.
     * <p>
     * Times out in 500ms.
     * <p>
     * Runs asynchronously.
     * <p>
     * Does not respect thread interruption.
     * <p>
     * Has a bulkhead with capacity of 1, queue size of 1.
     * 
     * @param waitingFuture future to wait for
     * @return a completed Future
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    public Future<Void> serviceTimeoutAsyncBulkheadQueueTimed(Future<?> waitingFuture) {
        while (true) {
            try {
                waitingFuture.get(5, SECONDS);
                return CompletableFuture.completedFuture(null);
            }
            catch (InterruptedException e) {
                // Ignore
            }
            catch (ExecutionException e) {
                Assert.fail("Waiting future threw exception", e);
            }
            catch (TimeoutException e) {
                return CompletableFuture.completedFuture(null);
            }
        }
    }
    
    /**
     * Waits for waitingFuture to complete, then returns.
     * <p>
     * Times out in 500ms.
     * <p>
     * Runs asynchronously.
     * <p>
     * Does not respect thread interruption.
     * <p>
     * Will do 2 retries with no delay.
     * <p>
     * Increments timeoutAsyncRetryCounter.
     * 
     * @param waitingFuture future to wait for
     * @return a completed Future
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Asynchronous
    @Retry(maxRetries = 2, delay = 0, jitter = 0)
    public Future<Void> serviceTimeoutAsyncRetry(Future<?> waitingFuture) {
        timeoutAsyncRetryCounter.incrementAndGet();
        while (true) {
            try {
                waitingFuture.get(5, SECONDS);
                return CompletableFuture.completedFuture(null);
            }
            catch (InterruptedException e) {
                // Ignore
            }
            catch (ExecutionException e) {
                Assert.fail("Waiting future threw exception", e);
            }
            catch (TimeoutException e) {
                return CompletableFuture.completedFuture(null);
            }
        }
    }
    
    private AtomicInteger timeoutAsyncRetryCounter = new AtomicInteger();
    
    /**
     * @return value of timeoutAsyncRetryCounter
     */
    public int getTimeoutAsyncRetryCounter() {
        return timeoutAsyncRetryCounter.get();
    }
    
    /**
     * Waits for waitingFuture to complete, then returns.
     * <p>
     * Times out in 500ms.
     * <p>
     * Runs asynchronously.
     * <p>
     * Does not respect thread interruption.
     * <p>
     * Will run the fallback method on exception
     * 
     * @param waitingFuture future to wait for
     * @return Future completed with "OK", or completed with "FALLBACK" if the fallback ran
     */
    @Timeout(value = 500, unit = ChronoUnit.MILLIS)
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    public Future<String> serviceTimeoutAsyncFallback(Future<?> waitingFuture) {
        while (true) {
            try {
                waitingFuture.get(5, SECONDS);
                return CompletableFuture.completedFuture("OK");
            }
            catch (InterruptedException e) {
                // Ignore
            }
            catch (ExecutionException e) {
                Assert.fail("Waiting future threw exception", e);
            }
            catch (TimeoutException e) {
                return CompletableFuture.completedFuture("TIMEDOUT");
            }
        }
    }
    
    public Future<String> fallback(Future<?> waitingFuture) {
        return CompletableFuture.completedFuture("FALLBACK");
    }

}
