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
package org.eclipse.microprofile.fault.tolerance.tck;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.FallbackClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.FallbackClient;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.FallbackWithBeanClient;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.MyBean;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.SecondStringFallbackHandler;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.StringFallbackHandler;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.StringFallbackHandlerWithBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test fallback was invoked correctly; fallback handler supporting CDI injection; type safety on fallback class.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
public class FallbackTest extends Arquillian {

    private @Inject FallbackClient fallbackClient;
    private @Inject FallbackWithBeanClient fallbackWithBeanClient;
    private @Inject FallbackClassLevelClient fallbackClassLevelClient;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftfallback.jar")
                        .addClasses(FallbackClient.class, FallbackWithBeanClient.class,
                                        FallbackClassLevelClient.class, StringFallbackHandler.class,
                                        SecondStringFallbackHandler.class,
                                        StringFallbackHandlerWithBean.class, MyBean.class)
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                        .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftFallback.war")
                        .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test that a Fallback service is driven after the specified number of retries are executed.
     * 
     * Each of serviceA and serviceB specify the same FallbackHandler. The test checks that the 
     * handler has been driven in the correct ExecutionContext and that the Service has been
     * executed the correct number of times.
     */
    @Test
    public void testFallbackSuccess() {

        try {
            String result = fallbackClient.serviceA();
            Assert.assertTrue(result.contains("serviceA"),
                            "The message should be \"fallback for serviceA\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceA should not throw a RuntimeException in testFallbackSuccess");
        }
        Assert.assertEquals(fallbackClient.getCounterForInvokingServiceA(), 2, "The execution count should be 2 (1 retry + 1)");
        try {
            String result = fallbackClient.serviceB();
            Assert.assertTrue(result.contains("serviceB"),
                            "The message should be \"fallback for serviceB\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceB should not throw a RuntimeException in testFallbackSuccess");
        }
        Assert.assertEquals(fallbackClient.getCounterForInvokingServiceB(), 3, "The execution count should be 3 (2 retries + 1)");
    }

    /**
     * A refinement on testFallbackSuccess to test that a bean may be injected in the FallbackHandler.
     * 
     * Each of serviceA and serviceB specify the same FallbackHandler. The test checks that the 
     * handler has been driven in the correct ExecutionContext and that the Service has been
     * executed the correct number of times.
     */
    @Test
    public void testFallbackWithBeanSuccess() {

        try {
            String result = fallbackWithBeanClient.serviceA();
            Assert.assertTrue(result.contains("serviceA"),
                            "The message should be \"fallback for serviceA\"");
            //MyBean should be injected to the fallbackA
            Assert.assertTrue(result.contains("34"),
                            "The message should be \"fallback for serviceA myBean.getCount()=34\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceA should not throw a RuntimeException in testFallbackWithBeanSuccess");
        }
        Assert.assertEquals(fallbackWithBeanClient.getCounterForInvokingServiceA(), 2, "The execution count should be 2 (1 retry + 1)");
        try {
            String result = fallbackWithBeanClient.serviceB();
            Assert.assertTrue(result.contains("serviceB"),
                            "The message should be \"fallback for serviceB\"");
            //MyBean should be injected to the fallbackA, MyBean is application scoped, 
            //so the same instance should be injected to the fallback handler
            Assert.assertTrue(result.contains("35"),
                            "The message should be \"fallback for serviceB myBean.getCount()=35\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceB should not throw a RuntimeException in testFallbackWithBeanSuccess");
        }
        Assert.assertEquals(fallbackWithBeanClient.getCounterForInvokingServiceB(), 3, "The execution count should be 3 (2 retries + 1)");
    }

    /**
     * Analogous to testFallbackSuccess with Class level annotations that are inherited by serviceA
     * but overridden by serviceB. 
     */
    @Test
    public void testClassLevelFallbackSuccess() {

        try {
            String result = fallbackClassLevelClient.serviceA();
            Assert.assertTrue(result.contains("serviceA"),
                            "The message should be \"fallback for serviceA\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceA should not throw a RuntimeException in testFallbackSuccess");
        }
        Assert.assertEquals(fallbackClassLevelClient.getCounterForInvokingServiceA(), 2, "The execution count should be 2 (1 retry + 1)");
        try {
            String result = fallbackClassLevelClient.serviceB();
            Assert.assertTrue(result.contains("second fallback for serviceB"),
                            "The message should be \"second fallback for serviceB\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceB should not throw a RuntimeException in testFallbackSuccess");
        }
        Assert.assertEquals(fallbackClassLevelClient.getCounterForInvokingServiceB(), 3, "The execution count should be 3 (2 retries + 1)");
    }
}
