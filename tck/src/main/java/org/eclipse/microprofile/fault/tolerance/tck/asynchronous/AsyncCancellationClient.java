/*
 *******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.fault.tolerance.tck.asynchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AsyncCancellationClient {

    private AtomicInteger serviceAsyncRetryAttempts = new AtomicInteger(0);

    @Asynchronous
    public Future<?> serviceAsync(Barrier barrier, AtomicBoolean wasInterrupted) {
        try {
            barrier.awaitInterruptably();
        } catch (InterruptedException e) {
            wasInterrupted.set(true);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Asynchronous
    @Retry(maxRetries = 5, delay = 0, jitter = 0)
    public Future<?> serviceAsyncRetry(Barrier barrier) throws InterruptedException {
        serviceAsyncRetryAttempts.incrementAndGet();
        barrier.awaitInterruptably();
        return CompletableFuture.completedFuture(null);
    }

    public int getServiceAsyncRetryAttempts() {
        return serviceAsyncRetryAttempts.get();
    }

    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    public Future<?> serviceAsyncBulkhead(Barrier barrier) {
        barrier.await();
        return CompletableFuture.completedFuture(null);
    }

}
