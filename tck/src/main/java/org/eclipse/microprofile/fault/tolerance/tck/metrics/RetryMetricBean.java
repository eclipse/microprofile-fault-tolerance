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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import java.time.Duration;

import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class RetryMetricBean {

    @Retry(maxRetries = 5)
    public void failSeveralTimes(int timesToFail, CallCounter counter) {
        counter.calls++;
        if (counter.calls <= timesToFail) {
            throw new TestException("call no. " + counter.calls);
        }
    }

    @Retry(maxRetries = -1, maxDuration = 1000, delay = 0, jitter = 0)
    public void failAfterDelay(Duration delay) throws InterruptedException {
        Thread.sleep(delay.toMillis());
        throw new TestException("Exception after duration: " + delay);
    }

    @Retry(maxRetries = 5, abortOn = NonRetryableException.class)
    public void failSeveralTimesThenNonRetryable(int timesToFail, CallCounter counter) {
        counter.calls++;
        if (counter.calls <= timesToFail) {
            throw new TestException("call no. " + counter.calls);
        }
        throw new NonRetryableException();
    }

    @Retry(maxRetries = 0)
    public void maxRetriesZero() {
        throw new TestException("Test exception for maxRetriesZero");
    }

    public static class CallCounter {
        private int calls = 0;
    }

    @SuppressWarnings("serial")
    public static class NonRetryableException extends RuntimeException {
    }

}
