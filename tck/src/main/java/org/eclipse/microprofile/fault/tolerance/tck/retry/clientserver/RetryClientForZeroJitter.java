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

import javax.enterprise.context.ApplicationScoped;
import java.sql.Connection;

import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * A client to check for proper processing of jitter = 0 on @Retry
 * 
 * @author <a href="mailto:doychin@dsoft-bg.com">Doychin Bondzhev</a>
 *
 */
@ApplicationScoped
public class RetryClientForZeroJitter {

    private long totalRetryTime = 0;

    private long previousTime = 0;

    private int retries = -1; // first call is normal call

    @Retry(jitter = 0)
    public Connection serviceA() {
        long currentTime = System.currentTimeMillis();
        totalRetryTime += previousTime > 0 ? currentTime - previousTime : 0;
        previousTime = currentTime;
        retries++;
        throw new RuntimeException();
    }

    public int getRetries() {
        return retries;
    }

    public long getTotalRetryTime() {
        return totalRetryTime;
    }
}
