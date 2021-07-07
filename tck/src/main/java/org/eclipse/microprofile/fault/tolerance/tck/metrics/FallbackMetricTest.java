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
import static org.testng.Assert.expectThrows;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.FallbackMetricBean.Action;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.FallbackMetricBean.NonFallbackException;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricDefinition.InvocationFallback;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

public class FallbackMetricTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricFallback.war")
                .addClasses(FallbackMetricBean.class, FallbackMetricHandler.class)
                .addPackage(Packages.UTILS)
                .addPackage(Packages.METRIC_UTILS)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Inject
    private FallbackMetricBean fallbackBean;

    @Test
    public void fallbackMetricMethodTest() {
        MetricGetter m = new MetricGetter(FallbackMetricBean.class, "doWork");
        m.baselineMetrics();

        fallbackBean.setFallbackAction(Action.PASS);
        fallbackBean.doWork(Action.PASS);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(0L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.doWork(Action.FAIL);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.setFallbackAction(Action.FAIL);
        expectTestException(() -> fallbackBean.doWork(Action.FAIL));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));

        fallbackBean.setFallbackAction(Action.PASS);
        expectThrows(NonFallbackException.class, () -> fallbackBean.doWork(Action.NON_FALLBACK_EXCEPTION));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));
    }

    @Test
    public void fallbackMetricHandlerTest() {
        MetricGetter m = new MetricGetter(FallbackMetricBean.class, "doWorkWithHandler");
        m.baselineMetrics();

        fallbackBean.setFallbackAction(Action.PASS);
        fallbackBean.doWorkWithHandler(Action.PASS);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(0L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.doWorkWithHandler(Action.FAIL);

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(0L));

        fallbackBean.setFallbackAction(Action.FAIL);
        expectTestException(() -> fallbackBean.doWorkWithHandler(Action.FAIL));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(0L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));

        fallbackBean.setFallbackAction(Action.PASS);
        expectThrows(NonFallbackException.class, () -> fallbackBean.doWorkWithHandler(Action.NON_FALLBACK_EXCEPTION));

        assertThat("successful without fallback",
                m.getInvocations(VALUE_RETURNED, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("successful with fallback", m.getInvocations(VALUE_RETURNED, InvocationFallback.APPLIED).delta(),
                is(1L));
        assertThat("failed without fallback",
                m.getInvocations(EXCEPTION_THROWN, InvocationFallback.NOT_APPLIED).delta(), is(1L));
        assertThat("failed with fallback", m.getInvocations(EXCEPTION_THROWN, InvocationFallback.APPLIED).delta(),
                is(1L));
    }

}
