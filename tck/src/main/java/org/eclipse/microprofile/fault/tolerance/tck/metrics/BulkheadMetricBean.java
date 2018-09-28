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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.util.ConcurrentExecutionTracker;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

@ApplicationScoped
public class BulkheadMetricBean {
    
    @Inject private ConcurrentExecutionTracker tracker;

    /**
     * Wait for {@code future} to be completed
     *
     * @param future to complete
     */
    @Bulkhead(2)
    public void waitFor(Future<?> future) {
        doWaitFor(future);
    }

    /**
     * Separate waitFor method for testing execution time histogram
     *
     * @param future to complete
     */
    @Bulkhead(2)
    public void waitForHistogram(Future<?> future) {
        doWaitFor(future);
    }

    /**
     * WaitFor method for testing async calls
     *
     * @param future to complete
     * @return a completed future set to null
     */
    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public Future<Void> waitForAsync(Future<?> future) {
        doWaitFor(future);
        return CompletableFuture.completedFuture(null);
    }
    
    private void doWaitFor(Future<?> future) {
        try {
            tracker.executionStarted();
            future.get();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Test was interrupted", e);
        }
        catch (ExecutionException e) {
            throw new RuntimeException("Passed Future threw an exception", e);
        }
        finally {
            tracker.executionEnded();
        }
    }
    
    /**
     * Wait for the given number of method executions to be running in this bean
     * <p>
     * This method will wait three seconds before returning an exception
     *
     * @param executions number of executions
     */
    public void waitForRunningExecutions(int executions) {
        tracker.waitForRunningExecutions(executions);
    }
}
