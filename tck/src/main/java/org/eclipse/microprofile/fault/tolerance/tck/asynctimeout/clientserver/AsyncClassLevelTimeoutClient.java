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
package org.eclipse.microprofile.fault.tolerance.tck.asynctimeout.clientserver;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;

/**
 * A client to demonstrate the combination of the @Retry and @Timeout annotations when applied at the Class level.
 * 
 * @author <a href="mailto:neilyoung@uk.ibm.com">Neil Young</a>
 *
 */
@RequestScoped
@Timeout(2000)
@Asynchronous
public class AsyncClassLevelTimeoutClient {
    
    
    /**
     * serviceA is a slow running service that will take 5 seconds in normal operation. Here it is
     * configured to time out after 2 seconds.
     * @return the result as a Future
     * @throws InterruptedException the interrupted exception
     */
    public Future<Connection> serviceA() throws InterruptedException {

        Connection conn = new Connection() {
            {
                Thread.sleep(TCKConfig.getConfig().getTimeoutInMillis(5000));
            }
            
            @Override
            public String getData() {
                return "serviceA DATA";
            }
        };

        return CompletableFuture.completedFuture(conn);
    }
}
