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

import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.eclipse.microprofile.faulttolerance.Asynchronous;

import jakarta.enterprise.context.RequestScoped;

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
     * Service an operation until waitCondition is completed or 1000 second timeout.
     * 
     * @param waitCondition
     *            Execution of this method will delay until the condition is finished
     * @return the result as a Future.
     */
    @Asynchronous
    public Future<Connection> service(Future<?> waitCondition) {
        return CompletableFutureHelper.toCompletableFuture(serviceCS(waitCondition, false));
    }

    /**
     * Service an operation until waitCondition is completed or 1000 second timeout. NOTE: This 1000 second timeout is
     * to ensure test timeout kicks in before the operation timeout for a better test error to be displayed.
     * 
     * @param waitCondition
     *            Execution of this method will delay until the condition is finished
     * @param throwException
     *            Whether the method should throw an exception (true) or return a stage completed with exception (false)
     * @return the result as a CompletionStage. It may be completed with InterruptedException if the thread is
     *         interrupted
     */
    public CompletionStage<Connection> serviceCS(Future<?> waitCondition, boolean throwException) {

        Throwable exception = null;
        try {
            waitCondition.get(1000, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            exception = e.getCause();
        } catch (InterruptedException | TimeoutException e) {
            exception = e;
        }

        if (exception != null) {
            if (throwException) {
                if (exception instanceof RuntimeException) {
                    throw (RuntimeException) exception;
                } else {
                    throw new RuntimeException(exception);
                }
            } else {
                return CompletableFutureHelper.failedFuture(exception);
            }
        }

        return CompletableFuture.completedFuture(new Connection() {
            @Override
            public String getData() {
                return "service DATA";
            }
        });

    }

    public CompletionStage<Connection> serviceCS(Future<?> waitCondition) {
        return serviceCS(waitCondition, false);
    }

}
