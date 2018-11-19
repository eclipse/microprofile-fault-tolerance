/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.interceptor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Creates the producer method that allows access to the order queue.
 * @author carlosdlr
 */

@ApplicationScoped
public class OrderQueueProducer {

    private final Queue<String> orderQueue = new ConcurrentLinkedQueue<>();

    /**
     * producer method that will be called by other beans when is needed
     * @return order queue
     */
    @Produces
    public synchronized Queue<String> getOrderQueue() {
        return orderQueue;
    }
}
