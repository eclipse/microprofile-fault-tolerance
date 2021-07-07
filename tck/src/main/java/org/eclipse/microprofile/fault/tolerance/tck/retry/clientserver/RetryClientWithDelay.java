/*
 *******************************************************************************
 * Copyright (c) 2016-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.lessThan;

import java.sql.Connection;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

/**
 * A client to demonstrate the delay configurations
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
@RequestScoped
public class RetryClientWithDelay {
    private int counterForInvokingConnenectionService;
    private long timestampForConnectionService = 0;
    private Set<Duration> delayTimes = new HashSet<>();

    // Expect delay between 0-800ms. Set limit to 1000ms to allow a small buffer
    private static final Duration MAX_DELAY = TCKConfig.getConfig().getTimeoutInDuration(1000);

    // There should be 0-800ms (jitter is -400ms - 400ms) delays between each invocation
    // there should be at least 4 retries
    @Retry(delay = 400, maxDuration = 3200, jitter = 400, maxRetries = 10) // Adjusted by config
    public Connection serviceA() {
        return connectionService();
    }

    // simulate a backend service
    private Connection connectionService() {
        // record the time delay between each invocation (should be 0-800ms)
        long currentTime = System.nanoTime();
        if (timestampForConnectionService != 0) {
            delayTimes.add(Duration.ofNanos(currentTime - timestampForConnectionService));
        }
        timestampForConnectionService = currentTime;

        counterForInvokingConnenectionService++;
        throw new RuntimeException("Connection failed");
    }

    public void assertDelayInRange() {
        assertThat("Delay longer than expected", delayTimes, everyItem(lessThan(MAX_DELAY)));
    }

    public int getRetryCountForConnectionService() {
        return counterForInvokingConnenectionService;
    }

}
