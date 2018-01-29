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

import java.time.temporal.ChronoUnit;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;

/**
 * A simple method level synchronous @Bulkhead bean that has a retry option, 
 * with a waitingTaskQueue value that should be ignored.
 * 
 * @author Andrew Pielage
 */
@ApplicationScoped
public class Bulkhead5RapidRetry12MethodSynchBean implements BulkheadTestBackend {
    
    @Override
    @Bulkhead(value = 5, waitingTaskQueue = 5)
    @Retry(retryOn =
     { BulkheadException.class }, delay = 1, delayUnit = ChronoUnit.MICROS,
     maxRetries = 0, maxDuration=999999 )
    public Future test(BackendTestDelegate action) throws InterruptedException {
        Utils.log("in business method of bean " + this.getClass().getName());
        return action.perform();
    }
}
