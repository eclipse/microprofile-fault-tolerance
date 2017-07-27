/**********************************************************************
* Copyright (c) 2017 Contributors to the Eclipse Foundation 
*
* See the NOTICES file(s) distributed with this work for additional
* information regarding copyright ownership.
*
* All rights reserved. This program and the accompanying materials 
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php
*
* SPDX-License-Identifier: Apache-2.0
**********************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronous10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronous3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassSemaphore10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronous10Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronous3Bean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousQueueingBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadTestBackend;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

/**
 * @author Gordon Hutchison
 */

public class BulkheadAsynchTest {

    /*
     * We use an executer service to simulate the parallelism of multiple
     * simultaneous requests
     */
    private static final int THREADPOOL_SIZE = 30;
    private ExecutorService xService = Executors.newFixedThreadPool(THREADPOOL_SIZE);

    /*
     * As the FaultTolerance annotation only work on business methods of
     * injected objects we need to inject a variety of these for use by the
     * tests below. The naming convention indicates if the annotation is on a
     * class or method, asynchronous or semaphore based, the size/value of
     * the @Bulkhead and whether we have queueing or not.
     */

    @Inject
    public BulkheadClassAsynchronousDefaultBean bhBeanClassAsynchronousDefault;
    @Inject
    public BulkheadMethodAsynchronousDefaultBean bhBeanMethodAsynchronousDefault;
    @Inject
    public BulkheadClassAsynchronous3Bean bhBeanClassAsynchronous3;
    @Inject
    public BulkheadMethodAsynchronous3Bean bhBeanMethodAsynchronous3;
    @Inject
    public BulkheadClassAsynchronous10Bean bhBeanClassAsynchronous10;
    @Inject
    public BulkheadMethodAsynchronous10Bean bhBeanMethodAsynchronous10;
    @Inject
    public BulkheadClassAsynchronousQueueingBean bhBeanClassAsynchronousQueueing;
    @Inject
    public BulkheadMethodAsynchronousQueueingBean bhBeanMethodAsynchronousQueueing;
    
    /**
     * This is the Arquillian deploy method that controls the contents of the war
     * that contains all the tests.
     * 
     * @return the test war "ftBulkheadTest.war"
     */
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadTest.jar")
                .addPackage(BulkheadClassSemaphore10Bean.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadTest.war").addAsLibrary(testJar);
        return war;
    }

    public BulkheadAsynchTest() {
    }

    /**
     * Tests the class asynchronous Bulkhead(10) This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that is
     * a member of a @Bulkhead(10) Class.
     */
    @Test()
    public void testBulkheadClassAsynchronous10() {
        loop(10, bhBeanClassAsynchronous10, 10);
        check();
    }

    private void check() {
        
    }

    private void loop(int i, BulkheadTestBackend bhBean, int j, int k) {
        // TODO Auto-generated method stub
        
    }

    /**
     * Tests the method asynchronous Bulkhead(10). This test will check that 10
     * and no more than 10 asynchronous calls are allowed into a method that has
     * an individual
     * 
     * @Bulkhead(10) annotation
     */
    @Test()
    public void testBulkheadMethodAsynchronous10() {
        loop(10, bhBeanMethodAsynchronous10, 10);
        check();
    }

    private void loop(int i, BulkheadTestBackend bhBean, int j) {
        // TODO Auto-generated method stub
        
    }

    /**
     * Tests the class asynchronous Bulkhead(3) This test will check that 3 and
     * no more than 3 asynchronous calls are allowed into a method that is a
     * member of a @Bulkhead(3) Class.
     */
    @Test()
    public void testBulkheadClassAsynchronous3() {
        loop(10, bhBeanClassAsynchronous3, 3);
        check();
    }

    /**
     * Tests the method asynchronous Bulkhead(3). This test will check that 3
     * and no more than 3 asynchronous calls are allowed into a method that has
     * an individual Bulkhead(3) annotation
     */
    @Test()
    public void testBulkheadMethodAsynchronous3() {
        loop(10, bhBeanMethodAsynchronous3, 3);
        check();
    }

    /**
     * Tests the basic class asynchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 calls get
     * into the bulkhead at once.
     */
    @Test()
    public void testBulkheadClassAsynchronousDefault() {
        loop(10, bhBeanClassAsynchronousDefault, 10);
        check();
    }

    /**
     * Tests the basic method asynchronous Bulkhead with defaulting value
     * parameter. This will check that more than 1 but less than 10 calls get
     * into the bulkhead at once.
     */
    @Test()
    public void testBulkheadMethodAsynchronousDefault() {
        loop(10, bhBeanMethodAsynchronousDefault, 10);
        check();
    }

    /**
     * Tests the queueing class asynchronous Bulkhead with value parameter 10.
     * This will check that more than 1 but less than 10 calls get into the
     * bulkhead at once but that 10 threads can queue to get into the bulkhead
     */
    @Test()
    public void testBulkheadClassAsynchronousQueueing10() {
        loop(20, bhBeanClassAsynchronousQueueing, 10, 20);
        check();
    }

    /**
     * Tests the queueing method asynchronous Bulkhead with value parameter 10.
     * This will check that more than 1 but less than 10 calls get into the
     * bulkhead at once but that 10 threads can queue to get into the bulkhead
     */
    @Test()
    public void testBulkheadMethodAsynchronousQueueing10() {
        loop(20, bhBeanMethodAsynchronousQueueing, 10, 20);
        check();
    }
}