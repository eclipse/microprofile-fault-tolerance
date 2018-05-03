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

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient;
import org.eclipse.microprofile.fault.tolerance.tck.fallback.clientserver.StringFallbackHandler;
import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
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
 * Test the impact of policies disabling through config.
 *
 * The test assumes that the container supports both the MicroProfile Configuration API and the MicroProfile
 * Fault Tolerance API. Some Fault tolerance policies are disabled through configuration on DisabledClient methods.
 *
 * @author <a href="mailto:antoine@sabot-durand.net">Antoine Sabot-Durand</a>
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 */
public class DisableAnnotationOnMethodsTest extends Arquillian {

    @Inject
    private DisableClient disableClient;

    @Inject
    private AsyncClient asyncClient;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftDisableMethods.jar")
            .addClasses(DisableClient.class, StringFallbackHandler.class, AsyncClient.class, Connection.class)
            .addAsManifestResource(new StringAsset(
              "org.eclipse.microprofile.fault.tolerance.tck.disableEnv.DisableClient/serviceA/Retry/enabled=false\n" +
              "org.eclipse.microprofile.fault.tolerance.tck.disableEnv.DisableClient/serviceB/Fallback/enabled=false\n" +
              "org.eclipse.microprofile.fault.tolerance.tck.disableEnv.DisableClient/serviceC/CircuitBreaker/enabled=false\n" +
              "org.eclipse.microprofile.fault.tolerance.tck.disableEnv.DisableClient/serviceD/Timeout/enabled=false\n" +
              "org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient/service/Asynchronous/enabled=false"),
              "microprofile-config.properties")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
            .create(WebArchive.class, "ftDisableMethods.war")
            .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test maxRetries on @Retry.
     *
     * ServiceA is annotated with maxRetries = 1 so serviceA is expected to execute 2 times but as Retry is disabled,
     * then no retries should be attempted.
     */
    @Test
    public void testRetryDisabled() {
        try {
            disableClient.serviceA();
            Assert.fail("serviceA should throw a RuntimeException in testRetryDisabled");
        } 
        catch (RuntimeException ex) {
            // Expected
        }
        Assert.assertEquals(disableClient.getRetryCountForConnectionService(), 1, "The max number of executions should be 1");
    }

    /**
     * Test that a Fallback service is ignored when service fails.
     *
     * ServiceB is annotated with maxRetries = 1 so serviceB is expected to execute 2 times (Retry is not disabled on the method)
     */
    @Test
    public void testFallbackDisabled() {
        Assert.assertThrows(RuntimeException.class, () -> disableClient.serviceB());
        Assert.assertEquals(disableClient.getCounterForInvokingServiceB(), 2, "The execution count should be 1 (0 retries + 1)");
    }

    /**
     * A test to exercise Circuit Breaker thresholds, with a default SuccessThreshold
     *
     * CircuitBreaker policy being disabled the policy shouldn't be applied
     */
    @Test
    public void testCircuitClosedThenOpen() {
        for (int i = 0; i < 7; i++) {

            try {
                disableClient.serviceC();
            }
            catch (RuntimeException ex) {
                // Expected
            } catch (Exception ex) {
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
     * would be expected to throw a TimeoutException. However, Timeout policy being disabled, no Timeout will occur and a
     * RuntimeException will be thrown after 3 seconds.
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

    /**
     * A test to check taht asynchronous is disabled
     *
     * In normal operation, asyncClient.service() is launched asynchronously. As Asynchronous operation was disabled via config,
     * test is expecting a synchronous operation.
     *
     * @throws InterruptedException
     */
    @Test
    public void testAsync() throws InterruptedException {
        long start = System.currentTimeMillis();
        asyncClient.service();
        Assert.assertTrue(System.currentTimeMillis()-start >= 1000);

    }
}
