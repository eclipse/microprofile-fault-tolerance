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
package org.eclipse.microprofile.fault.tolerance.tck.disableEnv;

import java.sql.Connection;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.StringFallbackHandler;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

/**
 * A client to determine the impact of the MP_Fault_Tolerance_NonFallback_Enabled environment variable
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
public class DisableClient {
    private int counterForInvokingConnenectionService = 0;
    private int counterForInvokingServiceB = 0;
    private int counterForInvokingServiceC = 0;

    /**
     * Invokes connection service and increases the counter for invocations the connection service
     * @return Always throws exception
     */
    @Retry(maxRetries = 1)
    public Connection serviceA() {
        try {
            Thread.sleep(100);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return connectionService();
    }
    
    /**
     * Invokes name service and increases the counter for invocations of serviceB
     * @return Always throws exception
     */
    @Retry(maxRetries = 1)
    @Fallback(StringFallbackHandler.class)
    public String serviceB() {
        counterForInvokingServiceB++;
        return nameService();
    }
       
    /**
     * Invokes connection service and increases the counter for invocations of serviceC and connection service
     * @return Always throws exception
     */
    @CircuitBreaker(successThreshold = 2, requestVolumeThreshold = 4, failureRatio = 0.75, delay = 50000)
    public Connection serviceC() {
        Connection conn = null;
        counterForInvokingServiceC++;
        conn = connectionService();

        return conn;
    }
    
    /**
     * serviceD uses the default Fault Tolerance timeout of 1 second.
     * @param timeToSleep How long should the execution take in millis
     * @return null or exception is raised
     */    
    @Timeout
    public Connection serviceD(long timeToSleep) {
        try {
            Thread.sleep(timeToSleep);
            throw new RuntimeException("Timeout did not interrupt");
        } 
        catch (InterruptedException e) {
            //expected
        }
        return null;
    }   
    
    private String nameService() {
        throw new RuntimeException("Connection failed");
    }
    
    private Connection connectionService() {
        counterForInvokingConnenectionService++;
        throw new RuntimeException("Connection failed");
    }
    
    public int getRetryCountForConnectionService() {
        return counterForInvokingConnenectionService;
    }
    
    public int getCounterForInvokingServiceB() {
        return counterForInvokingServiceB;
    }
    
    public int getCounterForInvokingServiceC() {
        return counterForInvokingServiceC;
    }
}
