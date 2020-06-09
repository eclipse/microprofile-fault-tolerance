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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A suite of test methods to test the parameters of bulkhead
 */
@ApplicationScoped
public class BulkheadConfigBean {
    
    private AtomicInteger tasksRunning = new AtomicInteger();
    
    @Bulkhead(value = 5)
    public void serviceValue(Future<?> waitingFuture) throws InterruptedException, ExecutionException, TimeoutException {
        tasksRunning.incrementAndGet();
        try {
            waitingFuture.get(1, TimeUnit.MINUTES);
        }
        finally {
            tasksRunning.decrementAndGet();
        }
    }
    
    @Bulkhead(value = 1, waitingTaskQueue = 5)
    @Asynchronous
    public Future<Void> serviceWaitingTaskQueue(Future<?> waitingFuture) throws InterruptedException, ExecutionException, TimeoutException {
        tasksRunning.incrementAndGet();
        try {
            waitingFuture.get(1, TimeUnit.MINUTES);
            return CompletableFuture.completedFuture(null);
        }
        finally {
            tasksRunning.decrementAndGet();
        }
    }
    
    public int getTasksRunning() {
        return tasksRunning.get();
    }

}
