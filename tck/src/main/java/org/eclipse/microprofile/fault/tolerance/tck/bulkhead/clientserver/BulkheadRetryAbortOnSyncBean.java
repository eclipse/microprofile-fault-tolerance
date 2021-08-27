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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import org.eclipse.microprofile.fault.tolerance.tck.util.Barrier;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Test to ensure that retries do not occur if BulkheadException is included in abortOn attribute.
 * <p>
 * Has a bulkhead of size 1
 * <p>
 * Retries 1 time on any exception except BulkheadException with 1 second delay
 */
@Retry(maxRetries = 1, delay = 1000, jitter = 0, abortOn = BulkheadException.class)
@Bulkhead(1)
@ApplicationScoped
public class BulkheadRetryAbortOnSyncBean {

    public void test(Barrier barrier) {
        barrier.await();
    }

}
