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

import java.io.IOException;
import java.sql.Connection;

import org.eclipse.microprofile.faulttolerance.Retry;

import jakarta.enterprise.context.RequestScoped;

/**
 * A client to demonstrate the specification of abortOn conditions at the Class level.
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
@RequestScoped
@Retry(abortOn = {IOException.class})
public class RetryClassLevelClientAbortOn {
    private int counterForInvokingConnenectionService;
    private int counterForInvokingWritingService;

    public Connection serviceA() {
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
     * serviceB is configured to retry on a RuntimeException. The WritingService throws RuntimeExceptions.
     */
    @Retry(abortOn = {RuntimeException.class})
    public void serviceB() {
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
}
