/*
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
 */
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectCbOpen;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.CircuitBreakerMetricBean.Result;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class CircuitBreakerMetricTest extends Arquillian {
    
    private static final long CB_CLOSE_TIMEOUT = 5L * 1000 * 1_000_000;

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricCircuitBreaker.war")
                .addClasses(CircuitBreakerMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }
    
    @Inject private CircuitBreakerMetricBean cbBean;
    
    @BeforeTest
    public void closeTheCircuit() throws Exception {

        // Condition is needed because BeforeTest runs on both client and server
        if (cbBean != null) {
            
            // Assume the circuit is open
            // Attempt to put successful work through it until it stops throwing CircuitBreakerOpenExceptions
            boolean circuitOpen = true;
            long startTime = System.nanoTime();
            while (circuitOpen && System.nanoTime() - startTime < CB_CLOSE_TIMEOUT) {
                try {
                    for (int i = 0; i < 2; i++) {
                        cbBean.doWork(Result.PASS);
                    }
                    circuitOpen = false;
                }
                catch (CircuitBreakerOpenException e) {
                    Thread.sleep(100);
                }
            }
            
            if (circuitOpen) {
                throw new RuntimeException("Timed out waiting for circuit breaker to close");
            }
        }
    }
    
    @Test
    public void testCircuitBreakerMetric() throws Exception {
        MetricGetter m = new MetricGetter(CircuitBreakerMetricBean.class, "doWork");
        m.baselineCounters();
        
        // First failure, circuit remains closed
        expectTestException(() -> cbBean.doWork(Result.FAIL));
        
        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCallsSucceededDelta(), is(0L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCallsFailedDelta(), is(1L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCallsPreventedDelta(), is(0L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpenedDelta(), is(0L));

        // Second failure, causes circuit to open
        expectTestException(() -> cbBean.doWork(Result.FAIL));
        
        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCallsSucceededDelta(), is(0L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCallsFailedDelta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCallsPreventedDelta(), is(0L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpenedDelta(), is(1L));
        
        // Circuit is open, causing failure
        expectCbOpen(() -> cbBean.doWork(Result.PASS));
        
        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCallsSucceededDelta(), is(0L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCallsFailedDelta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCallsPreventedDelta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpenedDelta(), is(1L));
        
        // Wait a while for the circuit to be half-open
        Thread.sleep(1500);
        
        // Lots of successful work, causing the circuit to close again 
        for (int i = 0; i < 2; i++) {
            cbBean.doWork(Result.PASS);
        }
        
        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCallsSucceededDelta(), is(2L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCallsFailedDelta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCallsPreventedDelta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpenedDelta(), is(1L));

        // exception that is considered a success
        expect(RuntimeException.class, () -> cbBean.doWork(Result.PASS_EXCEPTION));

        assertThat("circuitbreaker calls succeeded", m.getCircuitBreakerCallsSucceededDelta(), is(3L));
        assertThat("circuitbreaker calls failed", m.getCircuitBreakerCallsFailedDelta(), is(2L));
        assertThat("circuitbreaker calls prevented", m.getCircuitBreakerCallsPreventedDelta(), is(1L));
        assertThat("circuit breaker times opened", m.getCircuitBreakerOpenedDelta(), is(1L));

        // General metrics should be updated
        assertThat("invocations", m.getInvocationsDelta(), is(6L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(4L));
    }
}
