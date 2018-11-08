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

import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
 * Ensure that metrics are created correctly when a Fault Tolerance annotation is placed on the class rather than the method.
 */
public class ClassLevelMetricTest extends Arquillian {

    
    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricClassLevel.war")
                .addClasses(ClassLevelMetricBean.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        
        return war;
    }
    
    @Inject
    private ClassLevelMetricBean classLevelRetryBean;
    
    @Test
    public void testRetryMetricSuccessfulImmediately() {
        MetricGetter m = new MetricGetter(ClassLevelMetricBean.class, "failSeveralTimes");
        m.baselineCounters();
        
        classLevelRetryBean.failSeveralTimes(0); // Should succeed on first attempt
        
        assertThat("calls succeeded without retry", m.getRetryCallsSucceededNotRetriedDelta(), is(1L));
        assertThat("calls succeeded after retry", m.getRetryCallsSucceededRetriedDelta(), is(0L));
        assertThat("calls failed", m.getRetryCallsFailedDelta(), is(0L));
        assertThat("retries", m.getRetryRetriesDelta(), is(0L));
        
        // General metrics
        assertThat("invocations", m.getInvocationsDelta(), is(1L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(0L));
    }
    
    @Test
    public void testRetryMetricSuccessfulAfterRetry() {
        MetricGetter m = new MetricGetter(ClassLevelMetricBean.class, "failSeveralTimes");
        m.baselineCounters();
        
        classLevelRetryBean.failSeveralTimes(3); // Should retry 3 times, and eventually succeed
        
        assertThat("calls succeeded without retry", m.getRetryCallsSucceededNotRetriedDelta(), is(0L));
        assertThat("calls succeeded after retry", m.getRetryCallsSucceededRetriedDelta(), is(1L));
        assertThat("calls failed", m.getRetryCallsFailedDelta(), is(0L));
        assertThat("retries", m.getRetryRetriesDelta(), is(3L));
        
        // General metrics
        assertThat("invocations", m.getInvocationsDelta(), is(1L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(0L));
    }
    
    @Test
    public void testRetryMetricUnsuccessful() {
        MetricGetter m = new MetricGetter(ClassLevelMetricBean.class, "failSeveralTimes");
        m.baselineCounters();
        
        expectTestException(() -> classLevelRetryBean.failSeveralTimes(20)); // Should retry 5 times, then fail
        expectTestException(() -> classLevelRetryBean.failSeveralTimes(20)); // Should retry 5 times, then fail
        
        assertThat("calls succeeded without retry", m.getRetryCallsSucceededNotRetriedDelta(), is(0L));
        assertThat("calls succeeded after retry", m.getRetryCallsSucceededRetriedDelta(), is(0L));
        assertThat("calls failed", m.getRetryCallsFailedDelta(), is(2L));
        assertThat("retries", m.getRetryRetriesDelta(), is(10L));
        
        // General metrics
        assertThat("invocations", m.getInvocationsDelta(), is(2L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(2L));

    }

}
