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
package org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * A client to demonstrate the fallback after doing the maximum retries
 * 
 * @author <a href="mailto:neilyoung@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
public class FallbackClient {

    private int counterForInvokingServiceA = 0;
    private int counterForInvokingServiceB = 0;
    private int counterForInvokingServiceC = 0;
    private int counterForInvokingServiceD = 0;
    private int counterForInvokingServiceE = 0;

    public int getCounterForInvokingServiceA() {
        return counterForInvokingServiceA;
    }

    public int getCounterForInvokingServiceB() {
        return counterForInvokingServiceB;
    }

    public int getCounterForInvokingServiceC() {
        return counterForInvokingServiceC;
    }

    public int getCounterForInvokingServiceD() {
        return counterForInvokingServiceD;
    }

    public int getCounterForInvokingServiceE() {
        return counterForInvokingServiceE;
    }
    
    @Retry(maxRetries = 1)
    @Fallback(StringFallbackHandler.class)
    public String serviceA() {
        counterForInvokingServiceA++;
        return nameService();
    }

    @Retry(maxRetries = 2)
    @Fallback(StringFallbackHandler.class)
    public String serviceB() {
        counterForInvokingServiceB++;
        return nameService();
    }

    @Timeout(500)
    @Retry(maxRetries = 1)
    @Fallback(StringFallbackHandler.class)
    public String serviceC(long timeToSleep) {
        try {
            counterForInvokingServiceC++;
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }

    @Retry(maxRetries = 1)
    @Fallback(fallbackMethod = "fallbackForServiceD")
    public String serviceD() {
        counterForInvokingServiceD++;
        return nameService();
    }

    public String fallbackForServiceD() {
        return "fallback method for serviceD";
    }

    @Retry(maxRetries = 1)
    @Fallback(fallbackMethod = "fallbackForServiceE")
    public String serviceE(String name, Integer type) {
        counterForInvokingServiceE++;
        return nameService();
    }

    public String fallbackForServiceE(String name, Integer type) {
        return "fallback method for serviceE";
    }
    
    private String nameService() {
        throw new RuntimeException("Connection failed");
    }

}
