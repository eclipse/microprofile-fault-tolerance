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
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClassLevelClientWithDelay;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientDefaultSuccessThreshold;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientHigherSuccessThreshold;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientNoDelay;
import org.eclipse.microprofile.fault.tolerance.tck.circuitbreaker.clientserver.CircuitBreakerClientWithDelay;
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
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftCircuitBreaker.jar")
                .addClasses(CircuitBreakerClientWithDelay.class, CircuitBreakerClientNoDelay.class,
                        CircuitBreakerClassLevelClientWithDelay.class,
                        CircuitBreakerClientDefaultSuccessThreshold.class,
                        CircuitBreakerClientHigherSuccessThreshold.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml").as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftCircuitBreaker.war").addAsLibrary(testJar);
        return war;
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
        for (int i = 0; i < 7; i++) {

            try {
                clientForCBWithDelay.serviceA();

                if (i < 4) {
                    Assert.fail("serviceA should throw an Exception in testCircuitClosedThenOpen");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4
                if (i < 4) {
                    Assert.fail("serviceA should throw a RuntimeException in testCircuitClosedThenOpen");
                }
            } 
            catch (RuntimeException ex) {
                // Expected
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException in testCircuitClosedThenOpen");
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
        for (int i = 0; i < 7; i++) {
            try {
                // Pause to allow the circuit breaker to half-open on iteration
                // 4
                // This is conservative when the circuit breaker has a minimal
                // delay.
                if (i == 4) {
                    try {
                        Thread.sleep(500);
                    } 
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                clientForCBNoDelay.serviceA();
                if (i < 4) {
                    Assert.fail("serviceA should throw an Exception in testCircuitReClose");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // The CB delay has been set to 1 ms, so the CB should
                // transition to half-open on iteration 4 and we
                // should not see a CircuitBreakerOpenException
                Assert.fail("serviceA should throw a RuntimeException in testCircuitReClose");
            } 
            catch (RuntimeException ex) {
                // Expected
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException in testCircuitReClose");
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
     * 6         SUCCEED (CircuitBreaker will be re-closed as successThreshold is 2)
     * 7         RunTimeException
     * 8         RunTimeException
     * 9         RunTimeException
     * 10        RunTimeException
     * 11        CircuitBreakerOpenException
     *
     */
    @Test
    public void testCircuitDefaultSuccessThreshold() {
        for (int i = 1; i < 12; i++) {

            try {
                clientForCBDefaultSuccess.serviceA();

                if (i < 5 || (i > 6 && i < 12)) {
                    Assert.fail("serviceA should throw an Exception in testCircuitDefaultSuccessThreshold");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // Expected on execution 5 and iteration 10

                if (i < 5) {
                    Assert.fail("serviceA should throw a RuntimeException in testCircuitDefaultSuccessThreshold");
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
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail(
                        "serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitDefaultSuccessThreshold");
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
                    Assert.fail("serviceA should throw an Exception in testCircuitHighSuccessThreshold");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4 and iteration 10
                if (i < 5) {
                    Assert.fail("serviceA should throw a RuntimeException in testCircuitHighSuccessThreshold");
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
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail(
                        "serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitHighSuccessThreshold");
            }
        }
        int serviceAExecutions = clientForCBHighSuccess.getCounterForInvokingServiceA();

        Assert.assertEquals(serviceAExecutions, 7, "The number of serviceA executions should be 7");
    }

    /**
     * Analagous to testCircuitClosedThenOpen but using a Class level rather
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
        for (int i = 0; i < 7; i++) {

            try {
                clientForClassLevelCBWithDelay.serviceA();

                if (i < 4) {
                    Assert.fail("serviceA should throw an Exception in testClassLevelCircuitBase");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4

                if (i < 4) {
                    Assert.fail("serviceA should throw a RuntimeException in testClassLevelCircuitBase");
                }
            } 
            catch (RuntimeException ex) {
                // Expected
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceA should throw a RuntimeException in testClassLevelCircuitBase");
            }
        }

        int serviceAExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceAExecutions, 4, "The number of executions should be 4");
    }

    /**
     * Analagous to testCircuitClosedThenOpen but with a Class level annotation
     * specified that is overriden by a Method level annotation on serviceC.
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
        for (int i = 0; i < 7; i++) {
            try {
                clientForClassLevelCBWithDelay.serviceC();

                if (i < 2) {
                    Assert.fail("serviceC should throw an Exception in testClassLevelCircuitOverride");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4
                if (i < 2) {
                    Assert.fail("serviceC should throw a RuntimeException in testClassLevelCircuitOverride");
                }
            } 
            catch (RuntimeException ex) {
                // Expected
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceC should throw a RuntimeException in testClassLevelCircuitOverride");
            }
        }

        int serviceAExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceAExecutions, 2, "The number of executions should be 2");
    }

    /**
     * Analagous to testCircuitReClose but with a Class level annotation
     * specified that is overriden by a Method level annotation on serviceD.
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
        for (int i = 0; i < 7; i++) {
            try {
                // Pause to allow the circuit breaker to half-open on iteration
                // 4
                // This is conservative when the circuit breaker has a minimal
                // delay.
                if (i == 4) {
                    try {
                        Thread.sleep(500);
                    } 
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                clientForClassLevelCBWithDelay.serviceD();
                if (i < 4) {
                    Assert.fail("serviceA should throw an Exception in testClassLevelCircuitOverrideNoDelay");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // The CB delay has been set to 1 ms, so the CB should
                // transition to half-open on iteration 4 and we
                // should not see a CircuitBreakerOpenException
                Assert.fail("serviceD should throw a RuntimeException in testClassLevelCircuitOverrideNoDelay");
            } 
            catch (RuntimeException ex) {
                // Expected
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail("serviceD should throw a RuntimeException in testClassLevelCircuitOverrideNoDelay");
            }
        }
        int serviceAExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceAExecutions, 7, "The number of executions should be 7");
    }

    /**
     * Analagous to testCircuitClosedThenOpen but using a Class level rather
     * than method level annotation. In this test, ServiceA and ServiceB are
     * called alternately. As they do not override the Class Level
     * CircuitBreaker annotation, the CircuitBreaker instance will be shared and
     * the behaviour the same as testCircuitClosedThenOpen.
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
    public void testClassLevelCircuitTwoService() {
        String currentService = "B";
        for (int i = 0; i < 7; i++) {

            try {
                // Alternate method calls
                if (currentService.equals("B")) {
                    currentService = "A";
                    clientForClassLevelCBWithDelay.serviceA();
                } 
                else {
                    currentService = "B";
                    clientForClassLevelCBWithDelay.serviceB();
                }

                if (i < 4) {
                    Assert.fail("service" + currentService
                            + " should throw an Exception in testClassLevelCircuitTwoService");
                }
            } 
            catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4

                if (i < 4) {
                    Assert.fail("service" + currentService
                            + " should throw a RuntimeException in testClassLevelCircuitTwoService");
                }
            } 
            catch (RuntimeException ex) {
                // Expected
            } 
            catch (Exception ex) {
                // Not Expected
                Assert.fail("service" + currentService
                        + " should throw a RuntimeException in testClassLevelCircuitTwoService");
            }
        }

        int serviceExecutions = clientForClassLevelCBWithDelay.getCounterForInvokingService();

        Assert.assertEquals(serviceExecutions, 4, "The number of executions should be 4");
    }
}
