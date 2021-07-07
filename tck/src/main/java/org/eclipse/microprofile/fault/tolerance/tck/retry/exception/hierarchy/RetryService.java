/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.retry.exception.hierarchy;

import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E0;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E1;
import org.eclipse.microprofile.fault.tolerance.tck.exception.hierarchy.E2;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RetryService {
    private RetryStatus status = RetryStatus.NOT_YET_INVOKED;

    @Retry(retryOn = {E0.class, E2.class}, abortOn = E1.class, maxRetries = 1)
    public void serviceA(Throwable exception) throws Throwable {
        stateTransition();
        throw exception;
    }

    @Retry(retryOn = {Exception.class, E1.class}, abortOn = {E0.class, E2.class}, maxRetries = 1)
    public void serviceB(Throwable exception) throws Throwable {
        stateTransition();
        throw exception;
    }

    @Retry(retryOn = {E1.class, E2.class}, abortOn = E0.class, maxRetries = 1)
    public void serviceC(Throwable exception) throws Throwable {
        stateTransition();
        throw exception;
    }

    private void stateTransition() {
        if (status == RetryStatus.NOT_YET_INVOKED) {
            status = RetryStatus.FIRST_INVOCATION;
        } else if (status == RetryStatus.FIRST_INVOCATION) {
            status = RetryStatus.RETRIED_INVOCATION;
        }
    }

    public RetryStatus getStatus() {
        return status;
    }
}
