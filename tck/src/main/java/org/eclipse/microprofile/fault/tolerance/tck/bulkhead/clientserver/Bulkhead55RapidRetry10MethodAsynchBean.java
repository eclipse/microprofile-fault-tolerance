/*
 *******************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * A simple method level Asynchronous @Bulkhead bean that has a retry option.
 *
 * @author Gordon Hutchison
 * @author Andrew Rouse
 */
@ApplicationScoped
public class Bulkhead55RapidRetry10MethodAsynchBean {

    @Bulkhead(waitingTaskQueue = 5, value = 5)
    @Asynchronous
    @Retry(retryOn = BulkheadException.class, delay = 1, delayUnit = ChronoUnit.MICROS, jitter = 0, maxRetries = 10, maxDuration = 999999)
    public Future<?> test(Barrier barrier) {
        barrier.await();
        return CompletableFuture.completedFuture(null);
    }

};
