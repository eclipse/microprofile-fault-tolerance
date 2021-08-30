/*
 *******************************************************************************
 * Copyright (c) 2016-2019 Contributors to the Eclipse Foundation
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

import java.sql.Connection;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

/**
 * A client to demonstrate the delay configurations
 * 
 * @author <a href="mailto:bbaptista@tomitribe.com">Bruno Baptista</a>
 *
 */
@RequestScoped
public class RetryClientWithNoDelayAndJitter {
    private int counterForInvokingConnectionService = 0;
    private long timestampForConnectionService = 0L;
    private final List<Long> delayTimes = new ArrayList<>();

    // There should be 0-400ms (jitter is -400ms - 400ms but min value must be 0) delays between each invocation
    // there should be at least 8 retries
    @Retry(delay = 0, maxDuration = 3200, jitter = 400, maxRetries = 100)
    public Connection serviceA() {
        return connectionService();
    }

    // simulate a backend service
    private Connection connectionService() {
        // the time delay between each invocation should be 0-400ms
        long currentTime = System.nanoTime();
        if (timestampForConnectionService != 0) {
            Duration delayDuration = Duration.ofNanos(currentTime - timestampForConnectionService);
            delayTimes.add(delayDuration.toMillis());
        }
        timestampForConnectionService = currentTime;

        counterForInvokingConnectionService++;
        throw new RuntimeException("Connection failed");
    }

    public int getRetryCountForConnectionService() {
        return counterForInvokingConnectionService;
    }

    public int positiveDelays() {
        System.out.println("delays are: " + delayTimes);
        int count = 0;
        // ignore fast delays
        for (long delayTime : delayTimes) {
            if (delayTime > 5) {
                count++;
            }
        }
        return count;
    }
}
