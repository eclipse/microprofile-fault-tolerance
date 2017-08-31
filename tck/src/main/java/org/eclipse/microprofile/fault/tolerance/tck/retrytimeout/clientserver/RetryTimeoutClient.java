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
package org.eclipse.microprofile.fault.tolerance.tck.retrytimeout.clientserver;

import javax.enterprise.context.RequestScoped;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * A client to demonstrate the combination of the @Retry and @Timeout annotations.
 * 
 * @author <a href="mailto:neilyoung@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
public class RetryTimeoutClient {

    private int counterForInvokingServiceA = 0;

    public int getCounterForInvokingServiceA() {
        return counterForInvokingServiceA;
    }

    @Timeout(500)
    @Retry(maxRetries = 1)
    public String serviceA(long timeToSleep) {
        try {
            counterForInvokingServiceA++;
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }
}
