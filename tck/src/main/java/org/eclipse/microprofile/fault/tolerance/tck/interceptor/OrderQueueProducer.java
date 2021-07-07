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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Creates the order queue that will be used by the interceptors and the test bean to store the order in which this
 * components are called.
 * 
 * @author carlosdlr
 */

@ApplicationScoped
public class OrderQueueProducer {

    private final Queue<String> orderQueue = new ConcurrentLinkedQueue<>();

    /**
     * get the order queue that stores the interceptors call order.
     * 
     * @return order queue
     */
    public Queue<String> getOrderQueue() {
        return orderQueue;
    }
}
