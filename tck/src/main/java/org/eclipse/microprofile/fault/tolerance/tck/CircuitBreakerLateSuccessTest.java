/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import static org.eclipse.microprofile.fault.tolerance.tck.Misc.Ints.contains;

import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientDefaultSuccessThreshold;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test CircuitBreaker using different success/failure pattern.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */

public class CircuitBreakerLateSuccessTest extends Arquillian {

    private @Inject CircuitBreakerClientDefaultSuccessThreshold clientForCBDefaultSuccess;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreakerLateSuccess.jar")
                .addClasses(CircuitBreakerClientDefaultSuccessThreshold.class,
                        Misc.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "ftCircuitBreakerLateSuccess.war")
                .addAsLibrary(testJar);
    }

    /**
     * Analagous to testCircuitDefaultSuccessThreshold but with a different success/failure pattern for the service that
     * is called. In this case, the service succeeds in the last call in the rolling window but the circuit will still
     * open as the failureRatio has been breached.
     *
     * With requestVolumeThreshold = 4, failureRatio=0.75 and successThreshold = 1 the expected behaviour is,
     *
     * Execution Behaviour ========= ========= 1 RunTimeException 2 RunTimeException 3 RunTimeException 4 SUCCESS 5
     * CircuitBreakerOpenException Pause for longer than CircuitBreaker delay, so that it transitions to half-open 6
     * SUCCEED (CircuitBreaker will be re-closed as successThreshold is 1. The impact of the success of the service and
     * the closure of the Circuit is to reset the rolling failure window to an empty state. Therefore another 4 requests
     * need to be made - of which at least 3 need to fail - for the Circuit to open again) 7 RuntimeException 8
     * RunTimeException 9 RunTimeException 10 SUCCESS 11 CircuitBreakerOpenException
     *
     */
    @Test
    public void testCircuitLateSuccessDefaultSuccessThreshold() {

        // Reset the counter in serviceA, to ensure that the test's environment is reinitialized.
        clientForCBDefaultSuccess.setCounterForInvokingServiceA(0);

        for (int i = 1; i < 12; i++) {
            int[] successSet = new int[]{4, 5, 9};
            try {
                clientForCBDefaultSuccess.serviceA(successSet);
                if (!contains(new int[]{4, 6, 10}, i)) {
                    Assert.fail(
                            "serviceA should throw an Exception in testCircuitLateSuccessDefaultSuccessThreshold on iteration "
                                    + i);
                }
            } catch (CircuitBreakerOpenException cboe) {
                // Expected on execution 5 & 11
                if (!contains(new int[]{5, 11}, i)) {
                    Assert.fail("in serviceA no CircuitBreakerOpenException should be fired on iteration " + i);
                } else if (i == 5) {
                    // Pause to allow the circuit breaker to half-open
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 7, 8, 9}, i)) {
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
}
