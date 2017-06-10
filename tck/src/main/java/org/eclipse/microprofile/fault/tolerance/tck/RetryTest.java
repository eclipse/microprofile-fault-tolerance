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

import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientForMaxRetries;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientWithDelay;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.testng.annotations.Test;
/**
 * Test when maxDuration is reached, no more retries will be perfomed.
 * Test the delay and jitter were taken into consideration.
 * 
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public class RetryTest extends Arquillian {

    private @Inject RetryClientForMaxRetries clientForMaxRetry;
    private @Inject RetryClientWithDelay clientForDelay;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftRetry.jar")
                .addClasses(RetryClientForMaxRetries.class, RetryClientWithDelay.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetry.war")
                .addAsLibrary(testJar);
        return war;
    }
    
    @Test
    public void testRetryMaxRetries() {
        clientForMaxRetry.serviceA();
        Assert.assertEquals("The max number of execution should be 6", 6, clientForMaxRetry.getRetryCountForConnectionService());
    }
    
    @Test
    public void testRetryMaxDuration() {
        clientForMaxRetry.serviceB();
        //The writingservice invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        Assert.assertTrue("The max execution counter should be less than 11", clientForMaxRetry.getRetryCountForWritingService()< 11);
    }
    
    @Test
    public void testRetryWithDelay() {
        clientForDelay.serviceA();
        Assert.assertEquals("The max number of execution should be greater than 4",  clientForDelay.getRetryCountForConnectionService() > 4);
        Assert.assertTrue("The delay between each retry should be 0-800ms", clientForDelay.isDelayInRange());
    }
}
