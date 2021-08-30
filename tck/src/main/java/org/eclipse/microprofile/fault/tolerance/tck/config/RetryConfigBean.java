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

package org.eclipse.microprofile.fault.tolerance.tck.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Suite of methods for testing the various parameters of Retry
 */
@ApplicationScoped
public class RetryConfigBean {

    @Retry(delay = 0, jitter = 0)
    public void serviceMaxRetries(AtomicInteger counter) {
        counter.getAndIncrement();
        throw new TestException();
    }

    @Retry(maxDuration = 10000, durationUnit = ChronoUnit.MILLIS, maxRetries = 10000, delay = 200, jitter = 0)
    public void serviceMaxDuration() {
        throw new TestException();
    }

    @Retry(maxRetries = 5, delay = 2, delayUnit = ChronoUnit.SECONDS, jitter = 0)
    public void serviceDelay() {
        throw new TestException();
    }

    @Retry(maxRetries = 1, delay = 0, jitter = 0)
    public void serviceRetryOn(RuntimeException e, AtomicInteger counter) {
        counter.getAndIncrement();
        throw e;
    }

    @Retry(retryOn = {TestConfigExceptionA.class,
            TestConfigExceptionB.class}, abortOn = RuntimeException.class, maxRetries = 1, delay = 0, jitter = 0)
    public void serviceAbortOn(RuntimeException e, AtomicInteger counter) {
        counter.getAndIncrement();
        throw e;
    }

    private long lastStartTime = 0;

    /**
     * Method to detect whether jitter is enabled
     * <p>
     * Will throw TestConfigExceptionA if a delay &gt; 100ms is detected.
     * <p>
     * Otherwise will throw TestException which will cause a retry.
     * <p>
     * Limited to 10 seconds or 1000 retries, but will stop as soon as a delay of &gt; 100ms is observed.
     */
    @Retry(abortOn = TestConfigExceptionA.class, delay = 0, jitter = 0, maxRetries = 1000, maxDuration = 10, durationUnit = ChronoUnit.SECONDS)
    public void serviceJitter() {
        long startTime = System.nanoTime();
        if (lastStartTime != 0) {
            Duration delay = Duration.ofNanos(startTime - lastStartTime);
            // If delay > 100ms
            if (delay.compareTo(Duration.ofMillis(100)) > 0) {
                throw new TestConfigExceptionA();
            }
        }
        lastStartTime = startTime;
        throw new TestException();
    }
}
