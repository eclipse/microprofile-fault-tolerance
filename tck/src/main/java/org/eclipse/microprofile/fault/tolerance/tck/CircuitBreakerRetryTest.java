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

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClassLevelClientWithRetry;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithRetry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test CircuitBreaker Thresholds and delays with Retries.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */

public class CircuitBreakerRetryTest extends Arquillian {

    private @Inject CircuitBreakerClientWithRetry clientForCBWithRetry;
    private @Inject CircuitBreakerClassLevelClientWithRetry clientForClassLevelCBWithRetry;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreakerRetry.jar")
                        .addClasses(CircuitBreakerClientWithRetry.class, CircuitBreakerClassLevelClientWithRetry.class)
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                        .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftCircuitBreakerRetry.war")
                        .addAsLibrary(testJar);
        return war;
    }

    /**
     * A test to exercise Circuit Breaker thresholds with sufficient retries to open the
     * Circuit and result in a CircuitBreakerOpenException.
     */
    @Test
    public void testCircuitOpenWithMoreRetries() {
        int invokeCounter = 0;
        try {
            clientForCBWithRetry.serviceA();
            
            // serviceA should retry until the CB threshold is reached. At that point a CircuitBreakerOpenException
            // should be thrown. Assert if this does not happen.
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceA();
            Assert.fail("serviceA should retry in testCircuitOpenWithMoreRetries on iteration "
                            + invokeCounter);
        }
        catch (CircuitBreakerOpenException cboe) {
            // Expected on iteration 4
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceA();
            if (invokeCounter < 4) {
                Assert.fail("serviceA should retry in testCircuitOpenWithMoreRetries on iteration "
                                + invokeCounter);
            }
        }
        catch (Exception ex) {
            // Not Expected
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceA();
            Assert.fail("serviceA should retry or throw a CircuitBreakerOpenException in testCircuitOpenWithMoreRetries on iteration "
                            + invokeCounter);
        }

        invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceA();
        Assert.assertEquals(invokeCounter, 4, "The number of executions should be 4");
    }

    /**
     * A test to exercise Circuit Breaker thresholds with insufficient retries to open the
     * Circuit so that the Circuit remains closed and a RuntimeException is caught.
     */
    @Test
    public void testCircuitOpenWithFewRetries() {
        int invokeCounter = 0;
        try {
            clientForCBWithRetry.serviceB();
            
            // serviceB should retry until the CB threshold is reached. At that point a CircuitBreakerOpenException
            // should be thrown. Assert if this does not happen.
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceB();
            Assert.fail("serviceB should retry in testCircuitOpenWithFewRetries on iteration "
                            + invokeCounter);
        }
        catch (CircuitBreakerOpenException cboe) {
            // Not Expected
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceB();
            Assert.fail("serviceB should retry or throw a RuntimeException (not a CBOE) in testCircuitOpenWithFewRetries on iteration "
                            + invokeCounter);

        }
        catch (RuntimeException ex) {
            // Expected on iteration 3
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceB();
            if (invokeCounter < 3) {
                Assert.fail("serviceB should retry in testCircuitOpenWithFewRetries on iteration "
                                + invokeCounter);
            }
        }
        catch (Exception ex) {
            // Not Expected
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceB();
            Assert.fail("serviceB should retry or throw a RuntimeException in testCircuitOpenWithFewRetries on iteration "
                            + invokeCounter);
        }

        invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceB();
        Assert.assertEquals(invokeCounter, 3, "The number of executions should be 3");
    }

    /**
     * Analogous to testCircuitOpenWithMoreRetries with Class level @CircuitBreaker and @Retry annotations 
     * that are inherited by serviceA
     */
    @Test
    public void testClassLevelCircuitOpenWithMoreRetries() {
        int invokeCounter = 0;
        try {
            clientForClassLevelCBWithRetry.serviceA();
            
            // serviceA should retry until the CB threshold is reached. At that point a CircuitBreakerOpenException
            // should be thrown. Assert if this does not happen.
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceA();
            Assert.fail("serviceA should retry in testClassLevelCircuitOpenWithMoreRetries on iteration "
                            + invokeCounter);
        }
        catch (CircuitBreakerOpenException cboe) {
            // Expected on iteration 4
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceA();
            if (invokeCounter < 4) {
                Assert.fail("serviceA should retry in testClassLevelCircuitOpenWithMoreRetries on iteration "
                                + invokeCounter);
            }
        }
        catch (Exception ex) {
            // Not Expected
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceA();
            Assert.fail("serviceA should retry or throw a CircuitBreakerOpenException in testClassLevelCircuitOpenWithMoreRetries on iteration "
                            + invokeCounter);
        }

        invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceA();
        Assert.assertEquals(invokeCounter, 4, "The number of executions should be 4");
    }

    /**
     * Analogous to testCircuitOpenWithFewRetries with Class level @CircuitBreaker and @Retry annotations 
     * that are overridden by serviceB.
     */
    @Test
    public void testClassLevelCircuitOpenWithFewRetries() {
        int invokeCounter = 0;
        try {
            clientForClassLevelCBWithRetry.serviceB();
            
            // serviceB should retry until the CB threshold is reached. At that point a CircuitBreakerOpenException
            // should be thrown. Assert if this does not happen.
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceB();
            Assert.fail("serviceB should retry in testClassLevelCircuitOpenWithFewRetries on iteration "
                            + invokeCounter);
        }
        catch (CircuitBreakerOpenException cboe) {
            // Not Expected
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceB();
            Assert.fail("serviceB should retry or throw a RuntimeException (not a CBOE) in testClassLevelCircuitOpenWithFewRetries on iteration "
                            + invokeCounter);

        }
        catch (RuntimeException ex) {
            // Expected on iteration 3
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceB();
            if (invokeCounter < 3) {
                Assert.fail("serviceB should retry in testClassLevelCircuitOpenWithFewRetries on iteration "
                                + invokeCounter);
            }
        }
        catch (Exception ex) {
            // Not Expected
            invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceB();
            Assert.fail("serviceB should retry or throw a RuntimeException in testClassLevelCircuitOpenWithFewRetries on iteration "
                            + invokeCounter);
        }

        invokeCounter = clientForClassLevelCBWithRetry.getCounterForInvokingServiceB();
        Assert.assertEquals(invokeCounter, 3, "The number of executions should be 3");
    }
    
    /**
     * Analogous to testCircuitOpenWithMoreRetries but execution failures are caused by timeouts.
     */
    @Test
    public void testCircuitOpenWithMultiTimeouts() {
        int invokeCounter = 0;
        try {
            clientForCBWithRetry.serviceC(1000);
            
            // serviceC should retry until the CB threshold is reached. At that point a CircuitBreakerOpenException
            // should be thrown. Assert if this does not happen.
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceC();
            Assert.fail("serviceC should retry in testCircuitOpenWithMultiTimeouts on iteration "
                                + invokeCounter);
            
        }
        catch (CircuitBreakerOpenException cboe) {
            // Expected on iteration 4          
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceC();
            if (invokeCounter < 4) {
                Assert.fail("serviceC should retry in testCircuitOpenWithMultiTimeouts on iteration "
                                + invokeCounter);
            }
        }
        catch (Exception ex) {
            // Not Expected
            invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceC();
            Assert.fail("serviceC should retry or throw a CircuitBreakerOpenException in testCircuitOpenWithMultiTimeouts on iteration "
                            + invokeCounter + ", caught exception: " + ex);
        }

        invokeCounter = clientForCBWithRetry.getCounterForInvokingServiceC();
        Assert.assertEquals(invokeCounter, 4, "The number of executions should be 4");
    }
}
