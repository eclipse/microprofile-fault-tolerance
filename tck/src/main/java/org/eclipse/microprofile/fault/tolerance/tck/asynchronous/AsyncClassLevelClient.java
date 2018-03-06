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
package org.eclipse.microprofile.fault.tolerance.tck.asynchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.eclipse.microprofile.faulttolerance.Asynchronous;

/**
 * A client to demonstrate Asynchronous behaviour when @Asynchronous is applied at class level
 *
 * @author <a href="mailto:antoine@sabot-durand.net">Antoine Sabot-Durand</a>
 *
 */
@RequestScoped
@Asynchronous
public class AsyncClassLevelClient {


    /**
     * service 1 second in normal operation.
     * @return the result as a Future
     * @throws InterruptedException the interrupted exception
     */
    public Future<Connection> service() throws InterruptedException {

        Connection conn = new Connection() {
            {
                Thread.sleep(1000);
            }

            @Override
            public String getData() {
                return "service DATA";
            }
        };

        return CompletableFuture.completedFuture(conn);
    }
}
