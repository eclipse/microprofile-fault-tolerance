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
package org.eclipse.microprofile.fault.tolerance.tck.util;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.Dependent;

/**
 * Utility bean to track the number of concurrent executions of a method
 * <p>
 * The method being tracked needs to call {@link #executionStarted()} when it
 * starts and {@link #executionEnded()} when it's about to end.
 * 
 * <pre>
 * try {
 *     tracker.executionStarted();
 *     // whatever the method is meant to do
 * }
 * finally {
 *     tracker.executionEnded()
 * }
 * </pre>
 * <p>
 * Another method can then call {@link #waitForRunningExecutions(int)} to wait
 * for the expected number of executions to start.
 */
@Dependent
public class ConcurrentExecutionTracker {
    
    // The Atomicness is not important, this is just an integer holder
    private final AtomicInteger executionCount = new AtomicInteger(0);
    
    private static final long WAIT_TIMEOUT = 3L * 1000 * 1_000_000;

    /**
     * Wait for the given number of method executions to be running
     * <p>
     * This method will wait three seconds before returning an exception
     *
     * @param executions number of executions
     */
    public void waitForRunningExecutions(int executions) {
        synchronized (executionCount) {
            long startTime = System.nanoTime();
            try {
                while (executionCount.get() != executions && (System.nanoTime() - startTime) < WAIT_TIMEOUT) {
                    executionCount.wait(500);
                }
            }
            catch (InterruptedException e) {
                // Stop waiting
            }
            
            if (executionCount.get() != executions) {
                // Timed out waiting
                throw new RuntimeException("Timed out waiting for executions to start, expected " + executions + " but there were " + executionCount);
            }
        }
    }
    
    public void executionStarted() {
        synchronized (executionCount) {
            executionCount.incrementAndGet();
            executionCount.notifyAll();
        }
    }
    
    public void executionEnded() {
        synchronized (executionCount) {
            executionCount.decrementAndGet();
            executionCount.notifyAll();
        }
    }

}
