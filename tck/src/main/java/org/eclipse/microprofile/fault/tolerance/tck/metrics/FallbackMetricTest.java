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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import static org.eclipse.microprofile.fault.tolerance.tck.metrics.util.Exceptions.expectTestException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.metrics.FallbackMetricBean.Action;
import org.eclipse.microprofile.fault.tolerance.tck.metrics.util.MetricGetter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

public class FallbackMetricTest extends Arquillian {

    @Deployment
    public static WebArchive deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftMetricFallback.war")
                .addClasses(FallbackMetricBean.class)
                .addPackage(MetricGetter.class.getPackage());
        return war;
    }
    
    @Inject private FallbackMetricBean fallbackBean;
    
    @Test
    public void fallbackMetricTest() {
        MetricGetter m = new MetricGetter(FallbackMetricBean.class, "doWork");
        m.baselineCounters();
        
        fallbackBean.setFallbackAction(Action.PASS);
        fallbackBean.doWork(Action.PASS);
        
        assertThat("fallback calls", m.getFallbackCallsDelta(), is(0L));
        assertThat("invocations", m.getInvocationsDelta(), is(1L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(0L));
        
        fallbackBean.doWork(Action.FAIL);
        
        assertThat("fallback calls", m.getFallbackCallsDelta(), is(1L));
        assertThat("invocations", m.getInvocationsDelta(), is(2L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(0L));
        
        fallbackBean.setFallbackAction(Action.FAIL);
        expectTestException(() -> fallbackBean.doWork(Action.FAIL));
        
        assertThat("fallback calls", m.getFallbackCallsDelta(), is(2L));
        assertThat("invocations", m.getInvocationsDelta(), is(3L));
        assertThat("failed invocations", m.getInvocationsFailedDelta(), is(1L));
    }
}
