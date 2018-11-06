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


import org.eclipse.microprofile.fault.tolerance.tck.interceptor.CounterFactory.CounterId;
import org.eclipse.microprofile.fault.tolerance.tck.interceptor.CounterFactory.OrderId;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;

/**
 * A Client to demonstrate the interceptors ordering behavior of FT and CDI annotations
 * @author carlosdlr
 */
public class FaultToleranceInterceptorTest extends Arquillian {

    @Inject
    @CounterId("EarlyFtInterceptor")
    private AtomicInteger earlyInterceptorCounter;

    @Inject
    @CounterId("LateFtInterceptor")
    private AtomicInteger lateInterceptorCounter;

    @Inject
    @CounterId("serviceA")
    private AtomicInteger methodCounter;

    @Inject
    @OrderId("EarlyOrderFtInterceptor")
    private Queue<String> orderKeeper;

    @Inject
    private InterceptorComponent testInterceptor;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "interceptorFtCdi.jar")
            .addClasses(InterceptorComponent.class, EarlyFtInterceptor.class, LateFtInterceptor.class, CounterFactory.class)
             .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "interceptorFtCdi.war")
            .addAsLibrary(testJar);
    }

    /**
     * This test validates the interceptors execution order after call a method
     * annotated with Asynchronous FT annotation, using a queue type FIFO (first-in-first-out).
     * The head of the queue is that element that has been on the queue the longest time.
     * In this case is validating that the early interceptor is executed at first.
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testAsync() throws InterruptedException, ExecutionException {
        Future<String> result = testInterceptor.asyncGetString();
        assertEquals(result.get(), "OK");
        assertEquals(orderKeeper.peek(), "EarlyOrderFtInterceptor");
    }

    @Test
    public void testRetryInterceptors() {
        try {
            testInterceptor.serviceA();
            fail("Exception not thrown");
        }
        catch (Exception e) {

        } // Expected

        assertEquals(methodCounter.get(), 6, "methodCounter"); // Method called six times (1 + 5 retries)
        assertEquals(earlyInterceptorCounter.get(), 2, "earlyInterceptorCounter");
        assertEquals(lateInterceptorCounter.get(), 7, "lateInterceptorCounter");
    }
}
