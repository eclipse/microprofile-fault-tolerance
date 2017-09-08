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
package org.eclipse.microprofile.fault.tolerance.tck.disableEnv;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.StringFallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the impact of the MP_Fault_Tolerance_NonFallback_Enabled environment variable.
 * 
 * The test assumes that the container supports both the MicroProfile Configuration API and the MicroProfile
 * Fault Tolerance API. The MP_Fault_Tolerance_NonFallback_Enabled Variable is set to "false" in the manifest 
 * of the deployed application.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
public class DisableTest extends Arquillian {

    private @Inject DisableClient disableClient;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftDisableAllButFallback.jar")
                .addClasses(DisableClient.class, StringFallbackHandler.class)
                .addAsManifestResource(new StringAsset("MP_Fault_Tolerance_NonFallback_Enabled=false"),
                    "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftDisableAllButFallback.war")
                .addAsLibrary(testJar);
        return war;
    }
    
    /**
     * Test maxRetries on @Retry. 
     * 
     * ServiceA is annotated with maxRetries = 1 so serviceA is expected to execute 2 times but if MP_Fault_Tolerance_NonFallback_Enabled 
     * is set to false in the Container environment, then no retries should be attempted.
     */
    @Test
    public void testRetryDisabled() {
        try {
            disableClient.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryDisabled");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(disableClient.getRetryCountForConnectionService(), 1, "The max number of executions should be 1");
    }

    /**
     * Test that a Fallback service is driven when a Service fails.
     *  
     * ServiceB is annotated with maxRetries = 1 so serviceB is expected to execute 2 times but if MP_Fault_Tolerance_NonFallback_Enabled 
     * is set to false in the Container environment, then no retries should be attempted HOWEVER the Fallback should still be driven
     * successfully, so the test checks that a Fallback was driven after serviceB fails.
     */
    @Test
    public void testFallbackSuccess() {

        try {
            String result = disableClient.serviceB();
            System.out.println("testFallbackSuccess got result - " + result);
            Assert.assertTrue(result.contains("serviceB"),
                            "The message should be \"fallback for serviceB\"");
        }
        catch (RuntimeException ex) {
            Assert.fail("serviceB should not throw a RuntimeException in testFallbackSuccess");
        }
        Assert.assertEquals(disableClient.getCounterForInvokingServiceB(), 1, "The execution count should be 1 (0 retries + 1)");

    }
    
    /**
     * A test to exercise Circuit Breaker thresholds, with a default SuccessThreshold
     * 
     * If MP_Fault_Tolerance_NonFallback_Enabled is set to false in the Container environment, then the CircuitBreaker 
     * will not operate, no CircuitBreakerOpenExceptions will be thrown and execution will fail 7 times.
     */
    @Test
    public void testCircuitClosedThenOpen() {
        for (int i = 0; i < 7; i++) {

            try {
                disableClient.serviceC();
            }
            catch (RuntimeException ex) {
                // Expected
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceC should throw a RuntimeException in testCircuitClosedThenOpen on iteration " + i + 
                                " but caught exception " + ex);
            }
        }
        int serviceCExecutions = disableClient.getCounterForInvokingServiceC();

        Assert.assertEquals(serviceCExecutions, 7, "The number of executions should be 7");
    }
    
    /**
     * A test to exercise the default timeout. 
     * 
     * In normal operation, the default Fault Tolerance timeout is 1 second but serviceD will attempt to sleep for 3 seconds, so
     * would be expected to throw a TimeoutException. However, if MP_Fault_Tolerance_NonFallback_Enabled is set to false in the Container 
     * environment, then no Timeout will occur and a RuntimeException will be thrown after 3 seconds.
     * 
     */
    @Test
    public void testTimeout() {
        try {
            disableClient.serviceD(3000);
            Assert.fail("serviceD should throw a TimeoutException in testTimeout");
        } 
        catch (TimeoutException ex) {
            // Not Expected
            Assert.fail("serviceD should throw a RuntimeException in testTimeout not a TimeoutException");
        } 
        catch (RuntimeException ex) {
            // Expected       
        }
    }
}
