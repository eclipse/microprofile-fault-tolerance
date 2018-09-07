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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.context.RequestScoped;

import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.eclipse.microprofile.faulttolerance.Asynchronous;

/**
 * A client to demonstrate Asynchronous behaviour
 *
 * @author <a href="mailto:antoine@sabot-durand.net">Antoine Sabot-Durand</a>
 *
 */
@RequestScoped
public class AsyncClient {

    /**
     * service 1 second in normal operation.
     *
     * @return the result as a Future
     * @throws InterruptedException the interrupted exception
     */
    @Asynchronous
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

    /**
     * Service an operation until waitCondition is completed or 1 second timeout.
     *
     * @param waitCondition Execution of this method will delay until the condition is finished
     * @param throwException Whether the method should throw an exception (true) or return a stage completed with exception (false)
     * @return the result as a CompletionStage. It may be completed with
     * InterruptedException if the thread is interrupted
     */
    @Asynchronous
    public CompletionStage<Connection> serviceCS(Future<?> waitCondition, boolean throwException) {
        return serviceCS(waitCondition, false, null);
    }

    @Asynchronous
    public CompletionStage<Connection> serviceCS(Future<?> waitCondition) {
        return serviceCS(waitCondition, false);
    }
    
    @Asynchronous
    public CompletionStage<Connection> serviceCS(Future<?> waitCondition, 
            CompletionStage<Connection> stageToReturn) {
        return serviceCS(waitCondition, false, stageToReturn);
    }
    
    private CompletionStage<Connection> serviceCS(Future<?> waitCondition, boolean throwException, CompletionStage<Connection> stageToReturn) {

        Throwable exception = null;
        try {
            waitCondition.get(1000, TimeUnit.SECONDS);
        }
        catch (ExecutionException e) {
            exception = e.getCause();
        }
        catch (InterruptedException | TimeoutException e) {
            exception = e;
        }
        
        if (exception != null) {
            if (throwException) {
                throwAsRuntimeException(exception);
            }
            else {
                return CompletableFutureHelper.failedFuture(exception);
            }
        }

        if (stageToReturn != null) {
            return stageToReturn;
        }
        else {
            return CompletableFuture.completedFuture(new Connection() {
                @Override
                public String getData() {
                    return "service DATA";
                }
            });
        }

    }

    private void throwAsRuntimeException(Throwable exception) throws RuntimeException {
        if (exception instanceof RuntimeException) {
            throw (RuntimeException)exception;
        }
        else {
            throw new RuntimeException(exception);
        }
    }

}
