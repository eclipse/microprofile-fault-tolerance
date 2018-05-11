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

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@ApplicationScoped
public class ClashingNameBean {

    @Retry(maxRetries = 5)
    @Bulkhead(3)
    @Timeout(value = 1000, unit = ChronoUnit.MILLIS)
    @CircuitBreaker(failureRatio = 1.0, requestVolumeThreshold = 20)
    @Fallback(fallbackMethod = "doFallback")
    @Asynchronous
    public Future<Void> doWork() {
        return CompletableFuture.completedFuture(null);
    }
    
    @Retry(maxRetries = 5)
    @Bulkhead(3)
    @Timeout(value = 1000, unit = ChronoUnit.MILLIS)
    @CircuitBreaker(failureRatio = 1.0, requestVolumeThreshold = 20)
    @Fallback(fallbackMethod = "doFallback")
    @Asynchronous
    public Future<Void> doWork(String dummy) {
        return CompletableFuture.completedFuture(null);
    }
    
    public Future<Void> doFallback() {
        return CompletableFuture.completedFuture(null);
    }
    
    public Future<Void> doFallback(String dummy) {
        return CompletableFuture.completedFuture(null);
    }

}
