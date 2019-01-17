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
package org.eclipse.microprofile.fault.tolerance.tck;

import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTimeout;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.timeout.clientserver.UninterruptableTimeoutClient;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Test behaviour when a {@code @Timeout} is used but the method does not respond to interrupts.
 * <p>
 * This provokes a lot of edge case interactions between Timeout and other annotations.
 * <p>
 * Includes test for Timeout, Timeout + Async, Timeout + Async + Bulkhead, Timeout + Async + Retry.
 */
public class TimeoutUninterruptableTest extends Arquillian {

    @Deployment
    public static WebArchive deployment() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftTimeoutUninterruptable.jar")
                                        .addClass(UninterruptableTimeoutClient.class)
                                        .addPackage(Packages.UTILS)
                                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        WebArchive testWar = ShrinkWrap.create(WebArchive.class, "ftTimeoutUninterruptable.war")
                                       .addAsLibrary(testJar);

        return testWar;
    }

    private List<CompletableFuture<Void>> waitingFutures = new ArrayList<>();

    @Inject
    private UninterruptableTimeoutClient client;

    @Test
    public void testTimeout() {
        long startTime = System.nanoTime();
        expectTimeout(() -> client.serviceTimeout(1000));
        long endTime = System.nanoTime();
        
        // Interrupt flag should not be set
        assertFalse(Thread.interrupted(), "Thread was still interrupted when method returned");

        // Expect that execution will take at least the full 1000ms because the method does not respond to being
        // interrupted
        assertThat("Execution time (ns)", endTime - startTime, greaterThanOrEqualTo(Duration.ofMillis(1000).toNanos()));
    }

    @Test
    public void testTimeoutAsync() throws Exception {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        CompletableFuture<Void> completionFuture = new CompletableFuture<Void>();

        long startTime = System.nanoTime();
        Future<Void> result = client.serviceTimeoutAsync(waitingFuture, completionFuture);
        expect(TimeoutException.class, result);
        long resultTime = System.nanoTime();

        // We should get the TimeoutException after 500ms, allow up to 1500ms
        assertThat("Time for result to be complete", Duration.ofNanos(resultTime - startTime), lessThan(Duration.ofMillis(1500)));

        assertFalse(completionFuture.isDone(), "Method should still be running");

        // If we release the waitingFuture, the method should quickly complete
        waitingFuture.complete(null);
        completionFuture.get(2, TimeUnit.SECONDS);
    }
    
    @Test
    public void testTimeoutAsyncCS() throws InterruptedException {
        AtomicBoolean wasInterrupted = new AtomicBoolean(false);
        CompletableFuture<Void> completionFuture = new CompletableFuture<>();
        AtomicLong endTime = new AtomicLong();
        
        long startTime = System.nanoTime();
        client.serviceTimeoutAsyncCS(4000)
              .thenRun(() -> completionFuture.complete(null))
              .exceptionally((e) -> {
                  completionFuture.completeExceptionally(e);
                  return null;
              })
              .thenRun(() -> endTime.set(System.nanoTime()))
              .thenRun(() -> wasInterrupted.set(Thread.interrupted()));
        
        expect(TimeoutException.class, completionFuture);
        
        // Interrupt flag should not be set
        assertFalse(wasInterrupted.get(), "Thread was still interrupted when thenRun steps were run");
        
        // Expect that the method will timeout after 500ms, allow up to 1500ms
        assertThat("Execution time", Duration.ofNanos(endTime.get() - startTime), lessThan(Duration.ofMillis(1500)));

    }

    @Test
    public void testTimeoutAsyncBulkhead() throws InterruptedException {
        CompletableFuture<?> waitingFuture = newWaitingFuture();

        long startTime = System.nanoTime();
        Future<Void> resultA = client.serviceTimeoutAsyncBulkhead(waitingFuture);
        expect(TimeoutException.class, resultA);
        long resultTime = System.nanoTime();

        // Should get the TimeoutException after 500ms, allow up to 1500ms
        assertThat("Time for result to be complete", Duration.ofNanos(resultTime - startTime), lessThan(Duration.ofMillis(1500)));

        // Should record one execution
        assertEquals(client.getTimeoutAsyncBulkheadCounter(), 1, "Execution count after first call");

        // At this point, the first execution should still be running, so the next one should get queued
        startTime = System.nanoTime();
        Future<Void> resultB = client.serviceTimeoutAsyncBulkhead(waitingFuture);
        expect(TimeoutException.class, resultB);
        resultTime = System.nanoTime();

        // Should get the TimeoutException after 500ms, allow up to 1500ms
        assertThat("Time for result to be complete", Duration.ofNanos(resultTime - startTime), lessThan(Duration.ofMillis(1500)));

        // This time though, we shouldn't record a second execution since the request timed out before it got to start running
        assertEquals(client.getTimeoutAsyncBulkheadCounter(), 1, "Execution count after second call");

        // Make two more calls with a short gap
        Future<Void> resultC = client.serviceTimeoutAsyncBulkhead(waitingFuture);
        Thread.sleep(100);
        Future<Void> resultD = client.serviceTimeoutAsyncBulkhead(waitingFuture);

        // The first call should be queued and eventually time out
        // The second call should get a BulkheadException because the queue is full
        expect(TimeoutException.class, resultC);
        expect(BulkheadException.class, resultD);

        // Lastly, neither of these calls actually got to run, so there should be no more executions
        assertEquals(client.getTimeoutAsyncBulkheadCounter(), 1, "Execution count after fourth call");
        
        // Complete the waiting future and check that, after a short wait, we have no additional executions
        waitingFuture.complete(null);
        Thread.sleep(300);
        assertEquals(client.getTimeoutAsyncBulkheadCounter(), 1, "Execution count after completing all tasks");
    }
    
    /**
     * Test that the timeout timer is started when the execution is added to the queue
     * 
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testTimeoutAsyncBulkheadQueueTimed() throws InterruptedException {
        CompletableFuture<Void> waitingFutureA = newWaitingFuture();
        CompletableFuture<Void> waitingFutureB = newWaitingFuture();
        
        client.serviceTimeoutAsyncBulkheadQueueTimed(waitingFutureA);
        Thread.sleep(100);
        
        long startTime = System.nanoTime();
        Future<Void> resultB = client.serviceTimeoutAsyncBulkheadQueueTimed(waitingFutureB);
        
        Thread.sleep(300);
        
        // Allow call A to finish, this should allow call B to start
        waitingFutureA.complete(null);
        
        // Wait for call B to time out
        expect(TimeoutException.class, resultB);
        long endTime = System.nanoTime();
        
        // B should time out 500ms after it was submitted, even though it spent 300ms queued
        // Allow up to 750ms. Bound is tighter here as more than 800ms would represent incorrect behavior
        assertThat("Time taken for call B to timeout", Duration.ofNanos(endTime - startTime), lessThan(Duration.ofMillis(750)));
    }

    @Test
    public void testTimeoutAsyncRetry() {
        CompletableFuture<?> waitingFuture = newWaitingFuture();

        // Expect to run method three times, each one timing out after 500ms
        long startTime = System.nanoTime();
        Future<Void> result = client.serviceTimeoutAsyncRetry(waitingFuture);
        expect(TimeoutException.class, result);
        long resultTime = System.nanoTime();

        // Should get TimeoutException after 1500ms (3 attempts * 500ms), allow up to 3000ms
        assertThat("Time for result to complete", Duration.ofNanos(resultTime - startTime), lessThan(Duration.ofMillis(3000)));
        
        // Expect all executions to be run
        assertEquals(client.getTimeoutAsyncRetryCounter(), 3, "Execution count after one call");
    }
    
    /**
     * Test that the fallback is run as soon as the timeout occurs
     * 
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testTimeoutAsyncFallback() throws InterruptedException {
        CompletableFuture<?> waitingFuture = newWaitingFuture();
        
        long startTime = System.nanoTime();
        Future<String> resultFuture = client.serviceTimeoutAsyncFallback(waitingFuture);
        
        try {
            // Expect TimeoutException causing fallback to be invoked and the result returned
            assertEquals(resultFuture.get(1, TimeUnit.MINUTES), "FALLBACK");
        }
        catch (java.util.concurrent.TimeoutException e) {
            fail("Method did not complete", e);
        }
        catch (ExecutionException e) {
            fail("Unexpected exception thrown", e);
        }

        long resultTime = System.nanoTime();
        
        // Should get the TimeoutException + Fallback after 500ms, allow up to 1500ms
        assertThat("Time for result to be complete", Duration.ofNanos(resultTime - startTime), lessThan(Duration.ofMillis(1500)));
    }

    /**
     * Creates a waiting future and adds it to a list to be cleaned up at the end of the test
     * 
     * @return the waiting future
     */
    private CompletableFuture<Void> newWaitingFuture() {
        CompletableFuture<Void> waitingFuture = new CompletableFuture<Void>();
        waitingFutures.add(waitingFuture);
        return waitingFuture;
    }

    /**
     * Cleans up any waiting futures that have been created in the test
     */
    @AfterMethod
    public void cleanup() {
        for (CompletableFuture<Void> future : waitingFutures) {
            future.complete(null);
        }

        waitingFutures.clear();
    }

}
