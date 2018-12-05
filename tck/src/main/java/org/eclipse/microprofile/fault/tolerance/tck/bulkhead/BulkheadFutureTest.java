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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadClassAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadMethodAsynchronousDefaultBean;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.Checker;
import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.FutureChecker;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * This set of tests will test correct operation on the relevant methods of the
 * Future object that is returned from the business method of a Asynchronous
 * Method or Class.
 * 
 * @author Gordon Hutchison
 */
public class BulkheadFutureTest extends Arquillian {

    private static final int SHORT_TIME = 100;
    private static final int LONG_TIME = 3000;
    @Inject
    private BulkheadMethodAsynchronousDefaultBean bhBeanMethodAsynchronousDefault;
    @Inject
    private BulkheadClassAsynchronousDefaultBean bhBeanClassAsynchronousDefault;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadFutureTest.jar")
                .addPackage(FutureChecker.class.getPackage())
                .addClass(Utils.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadTest.war").addAsLibrary(testJar);
        return war;
    }

    @BeforeTest
    public void beforeTest(final ITestContext testContext) {
        Utils.log("Testmethod: " + testContext.getName());
    }

    /**
     * Tests that the Future that is returned from an asynchronous bulkhead
     * method can be queried for Done OK before and after a goodpath .get()
     */
    @Test()
    public void testBulkheadMethodAsynchFutureDoneAfterGet() {

        Checker fc = new FutureChecker(SHORT_TIME);
        Future<String> result = null;

        try {
            result = bhBeanMethodAsynchronousDefault.test(fc);
        }
        catch (InterruptedException e1) {
            Assert.fail("Unexpected interruption", e1);
        }

        Assert.assertFalse(result.isDone(), "Future reporting Done when not");
        try {
            String r;
            Assert.assertTrue("GET.".equals(r = result.get()), r);
            Assert.assertTrue("GET.GET_TO.".equals(r = result.get(1, TimeUnit.SECONDS)), r);
        }
        catch (Throwable t) {
            Assert.assertNull(t);
        }
        Assert.assertTrue(result.isDone(), "Future done not reporting true");
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead
     * method can be queried for Done OK even if the user never calls get() to
     * drive the backend (i.e. the method is called non-lazily)
     */
    @Test()
    public void testBulkheadMethodAsynchFutureDoneWithoutGet() {

        Checker fc = new FutureChecker(SHORT_TIME);
        Future<String> result = null;
        try {
            result = bhBeanMethodAsynchronousDefault.test(fc);
        }
        catch (InterruptedException e1) {
            Assert.fail("Unexpected interruption", e1);
        }

        Assert.assertFalse(result.isDone(), "Future reporting Done when not");
        try {
            Thread.sleep(SHORT_TIME + SHORT_TIME);
            // Usually, we will not enter the loop, 
            // but we are prepared to wait up to 100 SHORT_TIMES (10sec)
            int loops = 0;
            while(!result.isDone() && loops++ < 100) {
                Thread.sleep(SHORT_TIME);
            }
        }
        catch (Throwable t) {
            Assert.assertNull(t);
        }
        Assert.assertTrue(result.isDone(), "Future done not reporting true");
    }
  
    /**
     * Tests that the Future that is returned from a asynchronous bulkhead can
     * be queried for Done OK after a goodpath get with timeout and also
     * multiple gets can be called ok. This test is for the annotation at a
     * Class level.
     */
    @Test()
    public void testBulkheadClassAsynchFutureDoneAfterGet() {

        Checker fc = new FutureChecker(SHORT_TIME);
        Future<String> result = null;

        try {
            result = bhBeanClassAsynchronousDefault.test(fc);
        }
        catch (InterruptedException e1) {
            Assert.fail("Unexpected interruption", e1);
        }

        Assert.assertFalse(result.isDone(), "Future reporting Done when not");
        try {
            String r;
            Assert.assertTrue("GET_TO.".equals(r = result.get(1, TimeUnit.SECONDS)), r);
            Assert.assertTrue("GET_TO.GET.".equals(r = result.get()), r);

        }
        catch (Throwable t) {
            Assert.assertNull(t);
        }
        Assert.assertTrue(result.isDone(), "Future done not reporting true");
    }

    /**
     * Tests that the Future that is returned from a asynchronous bulkhead can
     * be queried for Done OK when get() is not called. This test is for the
     * annotation at a Class level.
     */
    @Test()
    public void testBulkheadClassAsynchFutureDoneWithoutGet() {

        Checker fc = new FutureChecker(SHORT_TIME);
        Future<String> result = null;
        try {
            result = bhBeanMethodAsynchronousDefault.test(fc);
        }
        catch (InterruptedException e1) {
            Assert.fail("Unexpected interruption", e1);
        }
        Assert.assertFalse(result.isDone(), "Future reporting Done when not");
        try {
            Thread.sleep(SHORT_TIME + SHORT_TIME);
            // Usually, we will not enter the loop, 
            // but we are prepared to wait up to 100 SHORT_TIMES (10sec)
            int loops = 0;
            while(!result.isDone() && loops++ < 100) {
                Thread.sleep(SHORT_TIME);
            }
        }
        catch (Throwable t) {
            Assert.assertNull(t);
        }
        Assert.assertTrue(result.isDone(), "Future done not reporting true");
    }

}