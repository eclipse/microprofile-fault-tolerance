/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.fault.tolerance.tck.config;

import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.ExceptionThrowingAction;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that the various parameters of Timeout can be configured
 */
public class TimeoutConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .set(TimeoutConfigBean.class, "serviceValue", Timeout.class, "value",
                        TCKConfig.getConfig().getTimeoutInStr(1000))
                // only changing value here to scale the original, not for the purpose of this test
                .set(TimeoutConfigBean.class, "serviceUnit", Timeout.class, "value",
                        TCKConfig.getConfig().getTimeoutInStr(1000))
                .set(TimeoutConfigBean.class, "serviceUnit", Timeout.class, "unit", "MILLIS")
                .set(TimeoutConfigBean.class, "serviceBoth", Timeout.class, "value",
                        TCKConfig.getConfig().getTimeoutInStr(1000))
                .set(TimeoutConfigBean.class, "serviceBoth", Timeout.class, "unit", "MILLIS");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftTimeoutConfig.jar")
                .addClasses(TimeoutConfigBean.class, CompletableFutureHelper.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap.create(WebArchive.class, "ftTimeoutConfig.war")
                .addAsLibrary(jar);
    }

    @Inject
    private TimeoutConfigBean bean;

    @Test
    public void testConfigValue() {
        // In annotation: value = 1
        // unit = MILLIS
        // In config: value = 1000
        doTest(() -> bean.serviceValue());
    }

    @Test
    public void testConfigUnit() {
        // In annotation: value = 1000
        // unit = MICROS
        // In config: unit = MILLIS
        doTest(() -> bean.serviceUnit());
    }

    @Test
    public void testConfigBoth() {
        // In annotation: value = 10
        // unit = MICROS
        // In config: value = 1000
        // unit = MILLIS
        doTest(() -> {
            try {
                CompletableFutureHelper.toCompletableFuture(bean.serviceBoth()).get(1, TimeUnit.MINUTES);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
                throw e;
            }
        });
    }

    private void doTest(ExceptionThrowingAction action) {
        long start = System.nanoTime();
        expect(TimeoutException.class, action);
        long end = System.nanoTime();

        long durationInMillis = Duration.ofNanos(end - start).toMillis();
        assertThat(durationInMillis, greaterThan(TCKConfig.getConfig().getTimeoutInMillis(800)));
        assertThat(durationInMillis, lessThan(TCKConfig.getConfig().getTimeoutInMillis(2000)));
    }
}
