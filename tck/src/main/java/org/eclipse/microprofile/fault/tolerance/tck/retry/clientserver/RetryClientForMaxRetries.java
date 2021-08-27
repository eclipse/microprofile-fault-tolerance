/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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
import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

/**
 * A client to demonstrate the maxRetries and max duration configuration
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
@RequestScoped
public class RetryClientForMaxRetries {
    private int counterForInvokingConnenectionService;
    private int counterForInvokingWritingService;
    private int counterForInvokingServiceA;
    private int counterForInvokingServiceB;
    private int counterForInvokingServiceC;

    @Retry(maxRetries = 5)
    public Connection serviceA() {
        counterForInvokingServiceA++;
        return connectionService();
    }

    private Connection connectionService() {
        counterForInvokingConnenectionService++;
        throw new RuntimeException("Connection failed");
    }

    public int getRetryCountForConnectionService() {
        return counterForInvokingConnenectionService;
    }

    /**
     * Max retries is configured to 90 but the max duration is 1 second with a default durationUnit of milliseconds.
     * 
     * Once the duration is reached, no more retries should be performed.
     */
    @Retry(maxRetries = 90, maxDuration = 1000)
    public void serviceB() {
        counterForInvokingServiceB++;
        writingService();
    }

    /**
     * Max retries is configured to 90 but the max duration is 1 second with a durationUnit of seconds specified.
     * 
     * Once the duration is reached, no more retries should be performed.
     */
    @Retry(maxRetries = 90, maxDuration = 1, durationUnit = ChronoUnit.SECONDS)
    public void serviceC() {
        counterForInvokingServiceC++;
        writingService();
    }

    private void writingService() {
        counterForInvokingWritingService++;
        try {
            Thread.sleep(100);
            throw new RuntimeException("WritingService failed");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public int getRetryCountForWritingService() {
        return counterForInvokingWritingService;
    }

    public int getRetryCounterForServiceA() {
        return counterForInvokingServiceA;
    }

    public int getRetryCounterForServiceB() {
        return counterForInvokingServiceB;
    }

    public int getRetryCounterForServiceC() {
        return counterForInvokingServiceC;
    }
}
