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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

/**
 * Test that metrics are created when all the Fault Tolerance annotations are placed on the same method
 */
public class AllMetricsTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricAll.war")
                .addClasses(AllMetricsBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        
        return war;
    }
    
    @Inject
    private AllMetricsBean allMetricsBean;
    
    @Test
    public void testAllMetrics() throws InterruptedException, ExecutionException {
        MetricGetter m = new MetricGetter(AllMetricsBean.class, "doWork");
        m.baselineCounters();
        
        allMetricsBean.doWork().get(); // Should succeed on first attempt
        
        // General metrics
        assertThat("invocations", m.getInvocationsDelta(), is(1L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(0L));
        
        // Retry metrics
        assertThat("calls succeeded without retry", m.getRetryCallsSucceededNotRetriedDelta(), is(1L));
        assertThat("calls succeeded after retry", m.getRetryCallsSucceededRetriedDelta(), is(0L));
        assertThat("calls failed", m.getRetryCallsFailedDelta(), is(0L));
        assertThat("retries", m.getRetryRetriesDelta(), is(0L));
        
        // Timeout metrics
        assertThat("timeout execution duration histogram present", m.getTimeoutExecutionDuration().isPresent(), is(true));
        assertThat("timed out calls", m.getTimeoutCallsTimedOutDelta(), is(0L));
        assertThat("non timed out calls", m.getTimeoutCallsNotTimedOutDelta(), is(1L));
        
        // CircuitBreaker metrics
        assertThat("circuitbreaker succeeded calls", m.getCircuitBreakerCallsSucceededDelta(), is(1L));
        assertThat("circuitbreaker failed calls", m.getCircuitBreakerCallsFailedDelta(), is(0L));
        assertThat("circuitbreaker prevented calls", m.getCircuitBreakerCallsPreventedDelta(), is(0L));
        assertThat("circuitbreaker closed time", m.getCircuitBreakerTimeClosedDelta(), greaterThan(0L));
        assertThat("circuitbreaker half open time", m.getCircuitBreakerTimeHalfOpenDelta(), is(0L));
        assertThat("circuitbreaker open time", m.getCircuitBreakerTimeOpenDelta(), is(0L));
        assertThat("circuitbreaker times opened", m.getCircuitBreakerOpenedDelta(), is(0L));
        
        // Bulkhead metrics
        assertThat("bulkhead concurrent executions", m.getBulkheadConcurrentExecutions().get(), is(0L));
        assertThat("bulkhead accepted calls", m.getBulkheadCallsAcceptedDelta(), is(1L));
        assertThat("bulkhead rejected calls", m.getBulkheadCallsRejectedDelta(), is(0L));
        assertThat("bulkhead duration histogram present", m.getBulkheadExecutionDuration().isPresent(), is(true));
        assertThat("bulkhead queue population present", m.getBulkheadQueuePopulation().isPresent(), is(true));
        assertThat("bulkhead queue wait time histogram present", m.getBulkheadWaitTime().isPresent(), is(true));
        
        // Fallback metrics
        assertThat("fallback calls", m.getFallbackCallsDelta(), is(0L));
    }
    
}
