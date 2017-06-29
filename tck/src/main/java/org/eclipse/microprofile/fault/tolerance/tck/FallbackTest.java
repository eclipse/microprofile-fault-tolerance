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

import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.FallbackA;
import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.FallbackClient;
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
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
public class FallbackTest extends Arquillian {

    private @Inject FallbackClient fallbackClient;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftfallback.jar")
                .addClasses(FallbackClient.class, FallbackA.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftFallback.war")
                .addAsLibrary(testJar);
        return war;
    }
    
    @Test
    public void testFallbackSuccess() {
        
        try {
            String result = fallbackClient.serviceA1();
            Assert.assertTrue(result.contains("serviceA1"), "The message should be \"fallback for serviceA1\"");
            //MyBean should be injected to the fallbackA
            Assert.assertTrue(result.contains("34"), "The message should be \"fallback for serviceA1 myBean.getCount()=34\"");
        }
        catch(RuntimeException ex) {
            Assert.fail("serviceA1 should not throw a RuntimeException in testRetryMaxRetries");
        }
        
        try {
            String result = fallbackClient.serviceA2();
            Assert.assertTrue(result.contains("serviceA2"), "The message should be \"fallback for serviceA2\"");
            //MyBean should be injected to the fallbackA, MyBean is application scoped, 
            //so the same instance should be injected to the fallback handler
            Assert.assertTrue(result.contains("35"), "The message should be \"fallback for serviceA2 myBean.getCount()=35\"");
        }
        catch(RuntimeException ex) {
            Assert.fail("serviceA2 should not throw a RuntimeException in testRetryMaxRetries");
        }
       
    }
    
    /**
     * The fallbakHandler is the wrong type. This should failed the validation check.
     */
    @Test
    public void testFallbackFailure() {
        try {
            fallbackClient.serviceB();
            Assert.fail("serviceB should throw an IllegalArgumentException in testFallbackFailure");
        }
        catch(RuntimeException ex) {
            
            Assert.assertTrue(ex instanceof IllegalArgumentException, "serviceB should throw an IllegalArgumentException in testFallbackFailure");
        }
        Assert.assertEquals(fallbackClient.getCounterForInvokingCountService(), 5, "The max number of execution should be 5");
        
    }
    
    
}
