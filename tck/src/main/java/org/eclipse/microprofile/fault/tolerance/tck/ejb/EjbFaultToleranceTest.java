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
package org.eclipse.microprofile.fault.tolerance.tck.ejb;


import org.eclipse.microprofile.fault.tolerance.tck.ejb.CounterFactory.CounterId;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;

/**
 * A Client to demonstrate the ordering behavior of FT annotation, CDI, and EJB interceptors
 *
 */
public class EjbFaultToleranceTest extends Arquillian {

    @Inject
    @CounterId("EarlyFtInterceptor")
    private AtomicInteger earlyInterceptorCounter;

    @Inject
    @CounterId("LateFtInterceptor")
    private AtomicInteger lateInterceptorCounter;

    @Inject
    @CounterId("serviceA")
    private AtomicInteger methodCounter;

    @EJB
    private EjbComponent testEjb;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "EjbFtCdi.jar")
            .addClasses(EjbComponent.class, EarlyFtInterceptor.class, LateFtInterceptor.class, CounterFactory.class)
             .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "EjbFtCdi.war")
            .addAsLibrary(testJar);
    }

    @Test
    public void testEjbRetryInterceptors() {
        try {
            testEjb.serviceA();
            fail("Exception not thrown");
        }
        catch (EJBException e) {

        } // Expected

        assertEquals(methodCounter.get(), 6, "methodCounter"); // Method called six times (1 + 5 retries)
        assertEquals(earlyInterceptorCounter.get(), 1, "earlyInterceptorCounter");
        assertEquals(lateInterceptorCounter.get(), 6, "lateInterceptorCounter");
    }
}
