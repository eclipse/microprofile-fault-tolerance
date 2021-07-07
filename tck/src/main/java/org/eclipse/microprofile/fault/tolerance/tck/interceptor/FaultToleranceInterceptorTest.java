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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * A Client to demonstrate the interceptors ordering behavior of FT and CDI annotations
 * 
 * @author carlosdlr
 */
public class FaultToleranceInterceptorTest extends Arquillian {

    @Inject
    private InterceptorComponent testInterceptor;

    @Inject
    private OrderQueueProducer orderFactory;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "interceptorFtCdi.jar")
                .addClasses(InterceptorComponent.class, EarlyFtInterceptor.class, LateFtInterceptor.class,
                        OrderQueueProducer.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "interceptorFtCdi.war")
                .addAsLibrary(testJar);
    }

    /**
     * This test validates the interceptors execution order after call a method annotated with Asynchronous FT
     * annotation, using a queue type FIFO (first-in-first-out). The head of the queue is that element that has been on
     * the queue the longest time. In this case is validating that the early interceptor is executed at first.
     * 
     * @throws InterruptedException
     *             if the current thread was interrupted while waiting
     * @throws ExecutionException
     *             if the computation threw an exception
     */
    @Test
    public void testAsync() throws InterruptedException, ExecutionException {
        Future<String> result = testInterceptor.asyncGetString();
        assertEquals(result.get(), "OK");
        String[] expectedOrder = {"EarlyOrderFtInterceptor", "LateOrderFtInterceptor", "asyncGetString"};
        assertEquals(orderFactory.getOrderQueue().toArray(), expectedOrder);
    }

    @Test
    public void testRetryInterceptors() {
        try {
            testInterceptor.serviceRetryA();
            fail("Exception not thrown");
        } catch (Exception e) {
            assertEquals(e.getMessage().trim(), "retryGetString failed");
        }

        String[] expectedOrder = {"EarlyOrderFtInterceptor", "LateOrderFtInterceptor", "serviceRetryA",
                "LateOrderFtInterceptor", "serviceRetryA"}; // executes 1 more time the later interceptor and the bean
                                                            // method
        // annotated with maxRetries = 1
        assertEquals(orderFactory.getOrderQueue().toArray(), expectedOrder);
    }

    @AfterMethod
    public void clearResources() {
        if (orderFactory != null) { // validate if not null because after the last test is called the context is cleared
            orderFactory.getOrderQueue().clear();
        }
    }
}
