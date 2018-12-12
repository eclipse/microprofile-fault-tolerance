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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.fail;

import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.Assert;

public class AbstractBulkheadTask {

    /**
     * Future that completes when the task starts running.
     */
    protected CompletableFuture<Void> runningLatch = new CompletableFuture<>();
    /**
     * Future that completes when {@link #complete()} is called
     * <p>
     * Execution of the task waits on this object
     */
    protected CompletableFuture<Future> releaseLatch = new CompletableFuture<>();
    
    /**
     * Future that is completed when the method returns with the value being {@code true} if the method was interrupted.
     */
    protected CompletableFuture<Boolean> interruptedLatch = new CompletableFuture<>();

    public AbstractBulkheadTask() {
        super();
    }

    /**
     * Wait until this task starts running
     * 
     * @param timeout time to wait
     * @param unit units of timeout
     * @throws InterruptedException if the thread is interrupted
     * @throws TimeoutException if the task does not start within the timeout
     */
    public void awaitRunning(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            runningLatch.get(timeout, unit);
        }
        catch (ExecutionException e) {
            // Don't expect an execution exception from a latch
            Assert.fail("Unexpected execution exception during awaitRunning", e);
        }
    }

    /**
     * Allow this task to complete
     */
    public void complete() {
        releaseLatch.complete(CompletableFuture.completedFuture("OK"));
    }

    /**
     * Allow this task to complete with the given result
     * @param result the result
     */
    public void complete(Future result) {
        releaseLatch.complete(result);
    }

    /**
     * Allow this task to complete by throwing an exception
     * 
     * @param e the exception
     */
    public void completeExceptionally(RuntimeException e) {
        releaseLatch.completeExceptionally(e);
    }

    /**
     * Asserts that the task starts within 2 seconds
     * 
     * @throws InterruptedException if the thread is interrupted while waiting for the task to start
     */
    public void assertStarting() throws InterruptedException {
        doAssertStarting();
    }

    /**
     * Asserts that the task does not start within 2 seconds
     * 
     * @throws InterruptedException if the thread is interrupted while waiting for the task to start
     */
    public void assertNotStarting() throws InterruptedException {
        try {
            awaitRunning(2, TimeUnit.SECONDS);
            Assert.fail("Task started unexpectedly");
        }
        catch (TimeoutException e) {
            // Expected
        }
    }
    
    /**
     * Asserts that all of the given tasks do not start within 2 seconds
     * <p>
     * If you have lots of tasks to check, this method will wait 2 seconds and check
     * all of them. This is much quicker than waiting 2 seconds for each task using
     * {@link #assertNotStarting()}.
     * 
     * @param tasks the tasks to check
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public static void assertAllNotStarting(Collection<? extends AbstractBulkheadTask> tasks) throws InterruptedException {
        Thread.sleep(2000);
        int i = 0;
        for (AbstractBulkheadTask task : tasks) {
            assertFalse(task.runningLatch.isDone(), "Task " + i + " is running.");
            i++;
        }
    }
    
    /**
     * The base implementation of {@link #assertStarting()}
     * <p>
     * This exists so superclasses can override {@link #assertStarting()} but this logic can still be called
     * 
     * @throws InterruptedException
     */
    private final void doAssertStarting() throws InterruptedException {
        try {
            awaitRunning(2, TimeUnit.SECONDS);
        }
        catch (TimeoutException e) {
            Assert.fail("Task did not start within 2 seconds");
        }
    }
    
    /**
     * Asserts that the task starts within 2 seconds
     * <p>
     * If the task doesn't start, this method will also report the status of
     * {@code methodResult}.
     * 
     * @param methodResult Future representing the result of a method expected to run this task
     * @throws InterruptedException if the thread is interrupted while waiting for the task to start
     */
    public void assertStarting(Future methodResult) throws InterruptedException {
        try {
            doAssertStarting();
        }
        catch (AssertionError err) {
            // Task has failed to start, check the result future to see if we can provide any more diagnostics
            if (methodResult.isDone()) {
                try {
                    Object result = methodResult.get(0, TimeUnit.SECONDS);
                    fail("Task did not start within 2 seconds. Method result: complete with result: " + result);
                }
                catch (CancellationException e) {
                    fail("Task did not start within 2 seconds. Method result: cancelled", e);
                }
                catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    fail("Task did not start within 2 seconds. Method result: failed with exception: " + cause, cause);
                }
                catch (InterruptedException e) {
                    fail("Task did not start within 2 seconds. "
                       + "Additionally, an InterruptedException was received when trying to check the method result",
                         e);
                }
                catch (TimeoutException e) {
                    fail("Task did not start within 2 seconds. "
                            + "Additionally, although the method result future reported done, "
                            + "a TimeoutException was received when trying to check the method result",
                              e);
                }
            }
            else {
                fail("Task did not start within 2 seconds. Method result: incomplete");
            }
        }
    }
    
    public void assertInterrupting() throws InterruptedException {
        try {
            boolean interrupted = awaitInterruptedResult(2, TimeUnit.SECONDS);
            if (!interrupted) {
                fail("Task completed without being interrupted");
            }
        }
        catch (TimeoutException e) {
            fail("Task had not been interrupted after two seconds", e);
        }
    }
    
    public void assertNotInterrupting() throws InterruptedException {
        try {
            boolean interrupted = awaitInterruptedResult(2, TimeUnit.SECONDS);
            if (interrupted) {
                fail("Task was interrupted within two seconds");
            }
        }
        catch (TimeoutException e) {
            // Expected
        }
    }
    
    public boolean awaitInterruptedResult(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            return interruptedLatch.get(time, unit);
        }
        catch (ExecutionException e) {
            // Don't expect an execution exception from a latch
            throw new AssertionError("Unexpected execution exception during awaitInterruptedResult", e);
        }
    }
    
    protected class TestDelegate implements BackendTestDelegate {
    
        @Override
        public Future perform() throws InterruptedException {
            runningLatch.complete(null);
            try {
                return releaseLatch.get(1, TimeUnit.MINUTES);
            }
            catch (ExecutionException e) {
                // Should be a runtime exception because that's all that completeExceptionally accepts
                throw (RuntimeException) e.getCause();
            }
            catch (TimeoutException e) {
                Assert.fail("Timeout waiting for release() to be called", e);
                return null; // Compiler requires a return even though fail() will throw an exception
            }
            catch (InterruptedException e) {
                interruptedLatch.complete(true);
                throw e;
            }
            finally {
                interruptedLatch.complete(false); // Ignored if the latch has already been completed
            }
        }
    }

}