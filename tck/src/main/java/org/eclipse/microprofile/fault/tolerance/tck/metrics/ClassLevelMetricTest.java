/*
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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

import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult.EXCEPTION_THROWN;
import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationResult.VALUE_RETURNED;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryResult;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.RetryRetried;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Ensure that metrics are created correctly when a Fault Tolerance annotation is placed on the class rather than the
 * method.
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
        m.baselineMetrics();

        classLevelRetryBean.failSeveralTimes(0); // Should succeed on first attempt

        assertRetryCallsIncremented(m, RetryRetried.FALSE, RetryResult.VALUE_RETURNED, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(0L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(1L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(0L));
    }

    @Test
    public void testRetryMetricSuccessfulAfterRetry() {
        MetricGetter m = new MetricGetter(ClassLevelMetricBean.class, "failSeveralTimes");
        m.baselineMetrics();

        classLevelRetryBean.failSeveralTimes(3); // Should retry 3 times, and eventually succeed

        assertRetryCallsIncremented(m, RetryRetried.TRUE, RetryResult.VALUE_RETURNED, 1L);
        assertThat("retries", m.getRetryRetries().delta(), is(3L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(1L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(0L));
    }

    @Test
    public void testRetryMetricUnsuccessful() {
        MetricGetter m = new MetricGetter(ClassLevelMetricBean.class, "failSeveralTimes");
        m.baselineMetrics();

        expectTestException(() -> classLevelRetryBean.failSeveralTimes(20)); // Should retry 5 times, then fail
        expectTestException(() -> classLevelRetryBean.failSeveralTimes(20)); // Should retry 5 times, then fail

        assertRetryCallsIncremented(m, RetryRetried.TRUE, RetryResult.MAX_RETRIES_REACHED, 2L);
        assertThat("retries", m.getRetryRetries().delta(), is(10L));

        assertThat("invocations returning value",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_DEFINED).delta(), is(0L));
        assertThat("invocations throwing exception",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_DEFINED).delta(), is(2L));
    }

    private void assertRetryCallsIncremented(MetricGetter m, RetryRetried retriedValue, RetryResult resultValue,
            Long expectedDelta) {
        for (RetryRetried retried : RetryRetried.values()) {
            for (RetryResult result : RetryResult.values()) {
                if (retried == retriedValue && result == resultValue) {
                    assertThat("Retry calls (" + retried + ", " + result + ")",
                            m.getRetryCalls(retried, result).delta(), is(expectedDelta));
                } else {
                    assertThat("Retry calls (" + retried + ", " + result + ")",
                            m.getRetryCalls(retried, result).delta(), is(0L));
                }
            }
        }
    }

}
