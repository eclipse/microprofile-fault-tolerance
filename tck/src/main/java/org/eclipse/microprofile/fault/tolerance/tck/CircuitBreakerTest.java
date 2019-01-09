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

import static org.eclipse.microprofile.fault.tolerance.tck.Misc.Ints.contains;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClassLevelClientWithDelay;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientDefaultSuccessThreshold;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientHigherSuccessThreshold;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientNoDelay;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientRollingWindow;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithDelay;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test CircuitBreaker Thresholds and delays.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */

public class CircuitBreakerTest extends Arquillian {

    private @Inject CircuitBreakerClientWithDelay clientForCBWithDelay;
    private @Inject CircuitBreakerClassLevelClientWithDelay clientForClassLevelCBWithDelay;
    private @Inject CircuitBreakerClientNoDelay clientForCBNoDelay;
    private @Inject CircuitBreakerClientDefaultSuccessThreshold clientForCBDefaultSuccess;
    private @Inject CircuitBreakerClientHigherSuccessThreshold clientForCBHighSuccess;

    private @Inject CircuitBreakerClientRollingWindow clientForRW;
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreaker.jar")
                        .addClasses(CircuitBreakerClientWithDelay.class,
                                        CircuitBreakerClientNoDelay.class,
                                        CircuitBreakerClassLevelClientWithDelay.class,
                                        CircuitBreakerClientDefaultSuccessThreshold.class,
                                        CircuitBreakerClientHigherSuccessThreshold.class,
                                        CircuitBreakerClientRollingWindow.class,
                                        Misc.class)
                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                        .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "ftCircuitBreaker.war")
                        .addAsLibrary(testJar);
    }

    /**
     * A test to exercise Circuit Breaker thresholds, with a default
     * SuccessThreshold
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.75, successThreshold = 2,
     * delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         RunTimeException 
     * 5         CircuitBreakerOpenException 
     * 6         CircuitBreakerOpenException 
     * 7         CircuitBreakerOpenException
     */
    @Test
    public void testCircuitClosedThenOpen() {
        for (int i = 1; i < 8; i++) {

            try {
                clientForCBWithDelay.serviceA();

                if (i < 5) {
                    Assert.fail("serviceA should throw an Exception in testCircuitClosedThenOpen on iteration " + i);
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 5
                if (i < 5) {
                    Assert.fail("serviceA should throw a RuntimeException in testCircuitClosedThenOpen on iteration " + i);
                }
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 4}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitClosedThenOpen on iteration " + i);
            }
        }
        int serviceAExecutions = clientForCBWithDelay.getCounterForInvokingServiceA();

        Assert.assertEquals(serviceAExecutions, 4, "The number of executions should be 4");
    }

    /**
     * A test to exercise Circuit Breaker thresholds, with a SuccessThreshold of
     * 2
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.75 and successThreshold =
     * 2 the expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         RunTimeException
     * Pause for longer than CircuitBreaker delay, so that it transitions to half-open
     * 5         SUCCEED 
     * 6         SUCCEED (CircuitBreaker will be re-closed as successThreshold is 2)
     * 7         SUCCEED
     */
    @Test
    public void testCircuitReClose() {
        for (int i = 1; i < 8; i++) {
            try {
                // Pause to allow the circuit breaker to half-open on iteration 5
                // This is conservative when the circuit breaker has a minimal delay.
                if (i == 5) {
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                clientForCBNoDelay.serviceA();
                if (i < 5) {
                    Assert.fail("serviceA should throw an Exception in testCircuitReClose on iteration " + i);
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // The CB delay has been set to 1 ms, so the CB should
                // transition to half-open on iteration 4 and we
                // should not see a CircuitBreakerOpenException
                Assert.fail("serviceA should throw a RuntimeException in testCircuitReClose on iteration " + i);
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 4}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should succeed or throw a RuntimeException in testCircuitReClose on iteration " + i);
            }
        }
        int serviceAExecutions = clientForCBNoDelay.getCounterForInvokingServiceA();

        Assert.assertEquals(serviceAExecutions, 7, "The number of executions should be 7");
    }

    /**
     * A test to exercise Circuit Breaker thresholds, with a default
     * SuccessThreshold
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.75 and successThreshold = 1 
     * the expected behaviour is,
     *
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         RunTimeException
     * 5         CircuitBreakerOpenException
     * Pause for longer than CircuitBreaker delay, so that it transitions to half-open
     * 6         SUCCEED (CircuitBreaker will be re-closed as successThreshold is 1. The impact of
     *                    the success of the service and the closure of the Circuit is to reset the
     *                    rolling failure window to an empty state. Therefore another 4 requests need
     *                    to be made - of which at least 3 need to fail - for the Circuit to open again)
     * 7         RunTimeException
     * 8         RunTimeException
     * 9         RunTimeException
     * 10        RuntimeException
     * 11        CircuitBreakerOpenException
     *
     */
    @Test
    public void testCircuitDefaultSuccessThreshold() {

        // Reset the counter in serviceA, to ensure that the test's environment is reinitialized.
        clientForCBDefaultSuccess.setCounterForInvokingServiceA(0);

        for (int i = 1; i < 12; i++) {
            int[] successSet = new int[]{5};
            try {
                clientForCBDefaultSuccess.serviceA(successSet);

                if (i != 6) {
                    Assert.fail("serviceA should throw an Exception in testCircuitDefaultSuccessThreshold on iteration " + i);
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected on execution 5 & 11
                if (!contains(new int[]{5, 11}, i)) {
                    Assert.fail("in serviceA no CircuitBreakerOpenException should be fired on iteration " + i);
                }
                else if (i == 5) {
                    // Pause to allow the circuit breaker to half-open
                    try {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 4, 7, 8 , 9, 10}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitDefaultSuccessThreshold "
                                + "on iteration " + i);
            }
        }
        int serviceAExecutions = clientForCBDefaultSuccess.getCounterForInvokingServiceA();

        Assert.assertEquals(serviceAExecutions, 9, "The number of serviceA executions should be 9");
    }

    /**
     * A test to exercise Circuit Breaker thresholds, with a default
     * SuccessThreshold
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.75 and successThreshold = 3
     * the expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         RunTimeException
     * 5         CircuitBreakerOpenException
     * Pause for longer than CircuitBreaker delay, so that it transitions to half-open
     * 6         SUCCEED 
     * 7         SUCCEED
     * 8         RunTimeException (CircuitBreaker will be re-opened)
     * 9         CircuitBreakerOpenException
     *
     */
    @Test
    public void testCircuitHighSuccessThreshold() {
        for (int i = 1; i < 10; i++) {

            try {
                clientForCBHighSuccess.serviceA();

                if (i < 5 || i > 7) {
                    Assert.fail("serviceA should throw an Exception in testCircuitHighSuccessThreshold on iteration " + i);
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 5 and iteration 9
                if (i == 5 || i == 9) {
                    if (i == 5) {
                        // Pause to allow the circuit breaker to half-open
                        try {
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else {
                    Assert.fail("serviceA should not throw a CircuitBreakerOpenException in testCircuitHighSuccessThreshold on iteration " + i);
                }
            }
            catch (RuntimeException ex) {
                // Check expected iterations
                if (!Misc.Ints.contains(new int[] {1, 2, 3, 4, 8}, i)) {
                    Assert.fail("serviceA should not have thrown a RuntimeException in testCircuitHighSuccessThreshold on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitHighSuccessThreshold "
                                + "on iteration " + i);
            }
        }
        int serviceAExecutions = clientForCBHighSuccess.getCounterForInvokingServiceA();

        Assert.assertEquals(serviceAExecutions, 7, "The number of serviceA executions should be 7");
    }

    /**
     * Analogous to testCircuitClosedThenOpen but using a Class level rather
     * than method level annotation.
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.75, successThreshold = 2
     * , delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         RunTimeException 
     * 5         CircuitBreakerOpenException 
     * 6         CircuitBreakerOpenException 
     * 7         CircuitBreakerOpenException
     */
    @Test
    public void testClassLevelCircuitBase() {
        for (int i = 1; i < 8; i++) {

            try {
                clientForClassLevelCBWithDelay.serviceA();

                if (i < 5) {
                    Assert.fail("serviceA should throw an Exception in testClassLevelCircuitBase");
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 5

                if (i < 5) {
                    Assert.fail("serviceA should throw a RuntimeException in testClassLevelCircuitBase on iteration " + i);
                }
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 4}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testClassLevelCircuitBase on iteration " + i);
            }
        }

        int serviceAExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceAExecutions, 4, "The number of executions should be 4");
    }

    /**
     * Analogous to testCircuitClosedThenOpen but with a Class level annotation
     * specified that is overridden by a Method level annotation on serviceC.
     * 
     * With successThreshold = 2, requestVolumeThreshold = 2, failureRatio=1,
     * delay = 50000 the expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         CircuitBreakerOpenException  
     * 4         CircuitBreakerOpenException 
     * 5         CircuitBreakerOpenException 
     * 6         CircuitBreakerOpenException 
     * 7         CircuitBreakerOpenException
     */
    @Test
    public void testClassLevelCircuitOverride() {
        for (int i = 1; i < 8; i++) {
            try {
                clientForClassLevelCBWithDelay.serviceC();

                if (i < 3) {
                    Assert.fail("serviceC should throw an Exception in testClassLevelCircuitOverride on iteration " + i);
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected starting from iteration 3
                if (i < 3) {
                    Assert.fail("serviceC should throw a RuntimeException in testClassLevelCircuitOverride on iteration " + i);
                }
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2}, i)) {
                    Assert.fail("serviceC should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceC should throw a RuntimeException or CircuitBreakerOpenException in testClassLevelCircuitOverride "
                                + "on iteration " + i);
            }
        }

        int serviceAExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceAExecutions, 2, "The number of executions should be 2");
    }

    /**
     * Analogous to testCircuitReClose but with a Class level annotation
     * specified that is overridden by a Method level annotation on serviceD.
     * 
     * With successThreshold = 2, requestVolumeThreshold = 4, failureRatio=0.75,
     * delay = 1 the expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         RunTimeException 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         RunTimeException
     * Pause for longer than CircuitBreaker delay, so that it transitions to half-open
     * 5         SUCCEED 
     * 6         SUCCEED (CircuitBreaker will be re-closed as successThreshold is 2)
     * 7         SUCCEED
     */
    @Test
    public void testClassLevelCircuitOverrideNoDelay() {
        for (int i = 1; i < 8; i++) {
            try {
                // Pause to allow the circuit breaker to half-open on iteration 5
                // This is conservative when the circuit breaker has a minimal
                // delay.
                if (i == 5) {
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                clientForClassLevelCBWithDelay.serviceD();
                if (i < 5) {
                    Assert.fail("serviceA should throw an Exception in testClassLevelCircuitOverrideNoDelay on iteration " + i);
                }
            }
            catch (CircuitBreakerOpenException cboe) {
                // The CB delay has been set to 1 ms, so the CB should
                // transition to half-open on iteration 4 and we
                // should not see a CircuitBreakerOpenException
                Assert.fail("serviceD should throw a RuntimeException in testClassLevelCircuitOverrideNoDelay on iteration " + i);
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{1, 2, 3, 4}, i)) {
                    Assert.fail("serviceD should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceD should succeed or throw a RuntimeException in testClassLevelCircuitOverrideNoDelay "
                                + "on iteration " + i);
            }
        }
        int serviceAExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceAExecutions, 7, "The number of executions should be 7");
    }
    
    /**
     * A test to exercise Circuit Breaker rolling window
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.5, expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         Success 
     * 2         RunTimeException 
     * 3         RunTimeException  
     * 4         Success 
     * 5         CircuitBreakerOpenException 
     */
    @Test
    public void testRollingWindowCircuitOpen() {
        boolean cbExceptionThrown = false;
        for (int i = 1; i < 6; i++) {

            try {
                clientForRW.service1RollingWindowOpenAfter4();
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 5
                cbExceptionThrown = true;
                if (i != 5) {
                    Assert.fail("serviceA should not throw a CircuitBreakerOpenException in testRollingWindowCircuitOpen on iteration " + i);
                }
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{2, 3}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testRollingWindowCircuitOpen "
                     + "on iteration " + i);
            }
        }
        int serviceAExecutions = clientForRW.getCounterForInvokingService1();
        Assert.assertTrue(cbExceptionThrown, "The CircuitBreaker exception in testRollingWindowCircuitOpen was not thrown correctly.");

        Assert.assertEquals(serviceAExecutions, 4, "The number of executions should be 4");
    }

    /**
     * A test to exercise Circuit Breaker rolling window
     * 
     * With requestVolumeThreshold = 4, failureRatio=0.5, expected behaviour is,
     * 
     * Execution Behaviour 
     * ========= ========= 
     * 1         Success 
     * 2         RunTimeException 
     * 3         Success  
     * 4         Success 
     * 5         RuntimeException
     * 6         CircuitBreakerOpenException 
     */
    @Test
    public void testRollingWindowCircuitOpen2() {
        boolean cbExceptionThrown = false;
        for (int i = 1; i < 7; i++) {

            try {
                clientForRW.service2RollingWindowOpenAfter5();
            }
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 5
                cbExceptionThrown = true;
                if (i != 6) {
                    Assert.fail("serviceA should not throw a CircuitBreakerOpenException in testRollingWindowCircuitOpen2 "
                            + "on iteration " + i);
                }
            }
            catch (RuntimeException ex) {
                // Expected
                if (!contains(new int[]{2, 5}, i)) {
                    Assert.fail("serviceA should not throw a RuntimeException on iteration " + i);
                }
            }
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testRollingWindowCircuitOpen2 "
                        + "on iteration " + i);
            }
        }
        int serviceAExecutions = clientForRW.getCounterForInvokingService2();
        Assert.assertTrue(cbExceptionThrown, "The CircuitBreaker exception in testRollingWindowCircuitOpen2 was not thrown correctly.");

        Assert.assertEquals(serviceAExecutions, 5, "The number of executions should be 5");
    }
}
