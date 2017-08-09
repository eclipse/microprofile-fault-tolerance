/*
 *******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

import java.util.concurrent.Future;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

/**
 * A simple class level Synchronous CircuitBreaker Bulkhead
 *
 * @author Gordon Hutchison
 */
@Bulkhead(value = 5, waitingTaskQueue = 5)
@CircuitBreaker(requestVolumeThreshold = 4, // evaluation window size
        failureRatio = 0.39, // If 39% of the evaluation window fails we
                             // blow/open
        successThreshold = 3, // Move out of half-open on three good calls
        delay = 1000)
public class BulkheadClassSynchronousCircuitBreakerBean implements BulkheadTestBackend {

    @Override
    public Future test(BackendTestDelegate action) throws InterruptedException {
        return action.perform();
    }

};