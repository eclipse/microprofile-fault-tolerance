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

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

import jakarta.enterprise.context.Dependent;

@Dependent
public class BulkheadCompletionStageBean {

    /**
     * Returns {@code stage} as the result
     * <p>
     * Allows two concurrent executions and two queued.
     * <p>
     * As this is an async method, it won't be considered "complete" by Fault Tolerance until {@code stage} completes.
     *
     * @param stage
     *            the CompletionStage to return as the result
     * @return {@code stage}
     */
    @Asynchronous
    @Bulkhead(value = 2, waitingTaskQueue = 2)
    public CompletionStage<Void> serviceCS(CompletionStage<Void> stage) {
        return stage;
    }

}
