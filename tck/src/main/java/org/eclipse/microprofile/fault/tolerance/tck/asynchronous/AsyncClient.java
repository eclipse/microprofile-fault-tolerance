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
import java.util.concurrent.Future;
import javax.enterprise.context.RequestScoped;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common.AsyncBridge;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common.Task;
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
     */
    @Asynchronous
    public Future<Task> service(Task task) {
        new AsyncBridge(task).perform(1000,"service DATA");
        return CompletableFuture.completedFuture(task);
    }

    /**
     * service 1 second in normal operation.
     *
     * @return the result as a {@link CompletionStage}
     */
    @Asynchronous
    public CompletionStage<Task> serviceCS(Task task) {
        new AsyncBridge(task).perform(1000,"service DATA");
        return CompletableFuture.completedFuture(task);
    }
}
