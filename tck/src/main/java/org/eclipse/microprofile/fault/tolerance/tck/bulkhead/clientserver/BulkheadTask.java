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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.testng.Assert;

/**
 * Manages the execution of a synchronous call to
 * {@link BulkheadTestBackend#test(BackendTestDelegate)}
 * <p>
 * This class is designed for testing synchronous bulkheads. When testing
 * synchronous bulkheads, you need multiple calls happening at once, so this
 * class starts an <i>asynchronous</i> task which calls into the bean from a
 * different thread.
 * <p>
 * Tasks start running and then wait for {@link #complete()} to be called. Some
 * time after this, the task will complete and the result can be retrieved with
 * {@link #getResult()} or {@link #getResultFuture()}.
 * <p>
 * There are also methods to assert that execution of the method is either
 * starting or not starting and finishing or not finishing. These methods assert
 * that the desired state is reached within a short period, to account for the
 * unpredictable ordering and timing of asynchronous execution.
 * <p>
 * Example use:
 * 
 * <pre>
 * <code>
 * BulkheadTask taskA = bulkheadTaskManager.startTask(beanUnderTest);
 * taskA.assertStarting();
 * taskA.complete(CompletableFuture.completedFuture("MyResult"));
 * taskA.assertFinishing();
 * assertThat(taskA.getResult().get(), is("MyResult"));
 * </code>
 * </pre>
 */
public class BulkheadTask extends AbstractBulkheadTask {
    
    private BulkheadTestBackend testBackend;
    
    private AsyncCaller executor;
    
    /**
     * Future that completes with the result of the method.
     * <p>
     * If the method throws an exception, this future will complete exceptionally.
     */
    private CompletableFuture<Future> resultFuture = new CompletableFuture<Future>();
    
    public BulkheadTask(AsyncCaller executor, BulkheadTestBackend testBackend) {
        super();
        this.executor = executor;
        this.testBackend = testBackend;
    }

    /**
     * Wait until this task finishes
     * 
     * @param timeout time to wait
     * @param unit units of timeout
     * @throws InterruptedException if the thread is interrupted
     * @throws TimeoutException if the task does not start within the timeout
     */
    public void awaitFinished(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        try {
            resultFuture.get(timeout, unit);
        }
        catch (ExecutionException e) {
            // Ignore exceptions, user just wants to know if we've finished
        }
    }
    
    /**
     * Get the result of the task
     * 
     * @return the result of the task
     * @throws ExecutionException if the task threw an exception
     */
    public Future getResult() throws ExecutionException {
        try {
            return resultFuture.get(0, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            Assert.fail("getResult() called when task not finished", e);
            return null; // compiler doesn't know that Assert.fail will always throw an exception
        }
        catch (InterruptedException e) {
            // Should not get interrupted because we asked not to wait
            Assert.fail("Unexpected InterruptedException when getting the result", e);
            return null; // compiler doesn't know that Assert.fail will always throw an exception
        }
    }
    
    /**
     * Returns a Future representing the result of this task
     * @return the future
     */
    public Future getResultFuture() {
        return resultFuture;
    }
    
    public void run() {
        executor.run(() -> {
            try {
                resultFuture.complete(testBackend.test(new TestDelegate()));
            }
            catch (Exception e) {
                resultFuture.completeExceptionally(e);
            }
        });
    }

    // Override assertStarting to provide diagnostics from resultFuture
    @Override
    public void assertStarting() throws InterruptedException {
        super.assertStarting(resultFuture);
    }

    public void assertFinishing() throws InterruptedException {
        try {
            awaitFinished(2, TimeUnit.SECONDS);
        }
        catch (TimeoutException e) {
            Assert.fail("Task did not finish within 2 seconds");
        }
    }
    
    public void assertNotFinishing() throws InterruptedException {
        try {
            awaitFinished(2, TimeUnit.SECONDS);
            Assert.fail("Task finished unexpectedly");
        }
        catch (TimeoutException e) {
        }
    }
    
    public void assertSuccessful() throws InterruptedException {
        try {
            getResult();
        }
        catch (ExecutionException e) {
            Assert.fail("Task did not complete successfully", e);
        }
    }
    
    public void assertThrows(Class<? extends Throwable> exceptionClazz) throws InterruptedException {
        try {
            getResult();
            Assert.fail("Task did not throw " + exceptionClazz.getName());
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (!exceptionClazz.isInstance(cause)) {
                Assert.fail("Unexpected exception thrown from task: " + cause, cause);
            }
        }
    }
}
