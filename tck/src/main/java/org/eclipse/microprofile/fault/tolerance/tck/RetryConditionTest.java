/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientAbortOn;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientRetryOn;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.testng.annotations.Test;
/**
 * Test the retryOn and abortOn conditions.
 * If retryOn condition is not met, no retry will be performed.
 * If abortOn condition is met, no retry will be performed.
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public class RetryConditionTest extends Arquillian {

    private @Inject RetryClientRetryOn clientForRetryOn;
    private @Inject RetryClientAbortOn clientForAbortOn;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftRetryCondition.jar")
                .addClasses(RetryClientAbortOn.class, RetryClientRetryOn.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetryCondition.war")
                .addAsLibrary(testJar);
        return war;
    }
    
    @Test
    public void testRetryOnTrue() {
        clientForRetryOn.serviceA();
        Assert.assertEquals("The invocation should only occur once.",  1, clientForRetryOn.getRetryCounterForServiceA());
        Assert.assertEquals("The max retry counter should be 3", 3, clientForRetryOn.getRetryCountForConnectionService());
    }
    
    @Test
    public void testRetryOnFalse() {
        clientForRetryOn.serviceB();
        Assert.assertEquals("The invocation should only occur once.",  1, clientForRetryOn.getRetryCounterForServiceB());
        Assert.assertEquals("The max invocation counter should be 1 as the retry condition is false", 1,  
                        clientForRetryOn.getRetryCountForWritingService());
    }
    
    @Test
    public void testRetryWithAbortOnFlase() {
        
        clientForAbortOn.serviceA();
        Assert.assertEquals("The invocation should only occur once.",  1, clientForAbortOn.getRetryCounterForServiceA());
        Assert.assertEquals("The max invocation should be 4 (3 retries + 1)", 4,  clientForAbortOn.getRetryCountForConnectionService());
        
    }
    
    @Test
    public void testRetryWithAbortOnTrue() {
        clientForRetryOn.serviceB();
        Assert.assertEquals("The invocation should only occur once.",  1, clientForAbortOn.getRetryCounterForServiceB());
        Assert.assertEquals("The max invocation counter should be 1 as the abort condition is true", 1,  
                        clientForAbortOn.getRetryCountForWritingService());
    }
}
