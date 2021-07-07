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

package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * 
 */
@ApplicationScoped
public class BulkheadPressureBean {

    private AtomicInteger inProgress = new AtomicInteger(0);
    private AtomicInteger maxInProgress = new AtomicInteger(0);

    @Bulkhead(5)
    public void servicePressure(long sleepTime) {
        int currentInProgress = inProgress.incrementAndGet();
        maxInProgress.getAndUpdate(v -> Math.max(v, currentInProgress));
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            fail("Sleep interrupted", e);
        } finally {
            inProgress.decrementAndGet();
        }
    }

    @Asynchronous
    @Bulkhead(value = 5, waitingTaskQueue = 5)
    public Future<?> servicePressureAsync(long sleepTime) {
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            fail("Sleep interrupted", e);
        }
        return CompletableFuture.completedFuture(null);
    }

    public int getMaxInProgress() {
        return maxInProgress.get();
    }

    public void reset() {
        assertEquals(inProgress.get(), 0);
        maxInProgress.set(0);
    }

}
