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

package org.eclipse.microprofile.fault.tolerance.tck.asynchronous;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.AsyncBulkheadTask;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;

@RequestScoped
public class AsyncCancellationClient {
    
    private AtomicInteger serviceAsyncRetryAttempts = new AtomicInteger(0);
    
    
    @Asynchronous
    public Future serviceAsync(AsyncBulkheadTask task) throws InterruptedException {
        return task.perform();
    }
    
    @Asynchronous
    @Retry(maxRetries = 5)
    public Future serviceAsyncRetry(AsyncBulkheadTask task) throws InterruptedException {
        serviceAsyncRetryAttempts.incrementAndGet();
        return task.perform();
    }
    
    public int getServiceAsyncRetryAttempts() {
        return serviceAsyncRetryAttempts.get();
    }
    
    
    @Asynchronous
    @Bulkhead(value = 1, waitingTaskQueue = 1)
    public Future serviceAsyncBulkhead(AsyncBulkheadTask task) throws InterruptedException {
        return task.perform();
    }

}
