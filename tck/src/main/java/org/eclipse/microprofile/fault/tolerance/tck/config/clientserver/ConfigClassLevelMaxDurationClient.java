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
package org.eclipse.microprofile.fault.tolerance.tck.config.clientserver;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.faulttolerance.Retry;

/**
 * A client to support Fault Tolerance Configuration tests.
 * 
 * @author <a href="mailto:neilyoung@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
@Retry(maxRetries = 90, maxDuration= 3000)
public class ConfigClassLevelMaxDurationClient {
 
    private int counterForInvokingWritingService = 0;
    
    /**
     * Max retries is configured to 90 but the max duration is 3 seconds with a default 
     * durationUnit of milliseconds.
     *  
     * Once the duration is reached, no more retries should be performed.
     */
    public void serviceA() {
        writingService();
    }    

    private void writingService() {
        counterForInvokingWritingService ++;
        try {
            Thread.sleep(100);
            throw new RuntimeException("WritingService failed");
        } 
        catch (InterruptedException e) {
            e.printStackTrace();
        }    
    }
    
    public int getRetryCountForWritingService() {
        return counterForInvokingWritingService;
    }    
}
