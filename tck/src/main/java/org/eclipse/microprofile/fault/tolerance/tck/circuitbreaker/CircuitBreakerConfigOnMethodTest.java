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
package org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker;

import static org.eclipse.microprofile.fault.tolerance.tck.Misc.Ints.contains;

import org.eclipse.microprofile.fault.tolerance.tck.Misc;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientDefaultSuccessThreshold;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test CircuitBreaker Thresholds and delays.
 *
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 */

public class CircuitBreakerConfigOnMethodTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreaker.jar")
                .addClasses(CircuitBreakerClientDefaultSuccessThreshold.class,
                        Misc.class)
                .addAsManifestResource(new StringAsset(
                        "org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker" +
                                ".clientserver.CircuitBreakerClientDefaultSuccessThreshold/serviceA/CircuitBreaker/delay=200"),
                        "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftCircuitBreaker.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * this test is a copy of CircuitBreakerTest#testCircuitDefaultSuccessThreshold() except that the waiting time to
     * let the Circuit Breaker is 500 ms. Without Property config the test can't pass
     */
    @Test
    public void testCircuitDefaultSuccessThreshold() {
        for (int i = 1; i < 12; i++) {
            int[] successSet = new int[]{5};
            try {
                clientForCBDefaultSuccess.serviceA(successSet);

                if (i != 6) {
                    Assert.fail("serviceA should throw an Exception in testCircuitDefaultSuccessThreshold on iteration "
                            + i);
                }
            } catch (CircuitBreakerOpenException cboe) {
                // Expected on execution 5 & 11
                if (!contains(new int[]{5, 11}, i)) {
                    Assert.fail("in serviceA no CircuitBreakerOpenException should be fired on iteration " + i);
                } else if (i == 5) {
                    // Pause to allow the circuit breaker to half-open
                    try {
                        Thread.sleep(500); // sleep to short to let cb to close with its annotation config.
                        // Will only pass thanks to property config
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 4, 7, 8, 9, 10}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            } catch (Exception ex) {
                // Not Expected
                Assert.fail(
                        "serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitDefaultSuccessThreshold "
                                + "on iteration " + i);
            }
        }
        int serviceAExecutions = clientForCBDefaultSuccess.getCounterForInvokingServiceA();

        Assert.assertEquals(serviceAExecutions, 9, "The number of serviceA executions should be 9");
    }

    private @Inject CircuitBreakerClientDefaultSuccessThreshold clientForCBDefaultSuccess;

}
