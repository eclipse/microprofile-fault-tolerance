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

import static org.awaitility.Awaitility.await;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expect;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectCbOpen;
import static org.eclipse.microprofile.fault.tolerance.tck.util.Exceptions.expectNoException;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test configuration of parameters of {@link CircuitBreaker}
 */
public class CircuitBreakerConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .set(CircuitBreakerConfigBean.class, "skipOnMethod", CircuitBreaker.class,
                        "skipOn", TestConfigExceptionA.class.getName())
                .set(CircuitBreakerConfigBean.class, "failOnMethod", CircuitBreaker.class,
                        "failOn", TestConfigExceptionA.class.getName())
                .set(CircuitBreakerConfigBean.class, "delayMethod", CircuitBreaker.class,
                        "delay", TCKConfig.getConfig().getTimeoutInStr(1000))
                .set(CircuitBreakerConfigBean.class, "delayMethod", CircuitBreaker.class,
                        "delayUnit", "MILLIS")
                .set(CircuitBreakerConfigBean.class, "requestVolumeThresholdMethod", CircuitBreaker.class,
                        "requestVolumeThreshold", "4")
                .set(CircuitBreakerConfigBean.class, "failureRatioMethod", CircuitBreaker.class,
                        "failureRatio", "0.8")
                .set(CircuitBreakerConfigBean.class, "successThresholdMethod", CircuitBreaker.class,
                        "successThreshold", "2")
                // only changing value here to scale the original, not for the purpose of this test
                .set(CircuitBreakerConfigBean.class, "successThresholdMethod", CircuitBreaker.class,
                        "delay", TCKConfig.getConfig().getTimeoutInStr(1000));

        JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, "ftCircuitBreakerConfig.jar")
                .addClasses(CircuitBreakerConfigBean.class, TestConfigExceptionA.class, TestConfigExceptionB.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        return ShrinkWrap
                .create(WebArchive.class, "ftCircuitBreakerConfig.war")
                .addAsLibraries(jar);
    }

    @Inject
    private CircuitBreakerConfigBean bean;

    @Test
    public void testConfigureFailOn() {
        // In annotation: requestVolumeThreshold = 2
        // skipOn = {}
        // failOn = {TestConfigExceptionB.class}
        // In config: failOn = {TestConfigExceptionA.class}

        expect(TestConfigExceptionA.class, () -> bean.failOnMethod());
        expect(TestConfigExceptionA.class, () -> bean.failOnMethod());

        // If failOn is not configured to include TestConfigExceptionA, this would throw a TestConfigExceptionA
        expectCbOpen(() -> bean.failOnMethod());
    }

    @Test
    public void testConfigureSkipOn() {
        // In annotation: requestVolumeThreshold = 2
        // failOn = {Throwable.class}
        // skipOn = {}
        // In config: skipOn = {TestConfigExceptionA.class}

        expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());
        expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());

        // If skipOn is not configured to include TestConfigExceptionA, this would throw a CircuitBreakerOpenException
        expect(TestConfigExceptionA.class, () -> bean.skipOnMethod());
    }

    @Test
    public void testConfigureDelay() {
        // In annotation: requestVolumeThreshold = 2
        // delay = 20
        // delayUnit = MICROS
        // In config: delay = 1000
        // delayUnit = MILLIS

        expect(TestConfigExceptionA.class, () -> bean.delayMethod(true));
        expect(TestConfigExceptionA.class, () -> bean.delayMethod(true));

        expectCbOpen(() -> bean.delayMethod(false));
        // CB is now open, wait until it moves to half-open (and time how long that took)

        long start = System.nanoTime();
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            expectNoException(() -> bean.delayMethod(false));
        });
        long end = System.nanoTime();

        long durationInMillis = Duration.ofNanos(end - start).toMillis();
        assertThat(durationInMillis, greaterThan(TCKConfig.getConfig().getTimeoutInMillis(800)));
        assertThat(durationInMillis, lessThan(TCKConfig.getConfig().getTimeoutInMillis(2000)));
    }

    @Test
    public void testConfigureRequestVolumeThreshold() {
        // In annotation: requestVolumeThreshold = 2
        // In config: requestVolumeThreshold = 4

        expect(TestConfigExceptionA.class, () -> bean.requestVolumeThresholdMethod());
        expect(TestConfigExceptionA.class, () -> bean.requestVolumeThresholdMethod());

        // If requestVolumeThreshold is not configured to 4, this would throw a CircuitBreakerOpenException
        expect(TestConfigExceptionA.class, () -> bean.requestVolumeThresholdMethod());
        expect(TestConfigExceptionA.class, () -> bean.requestVolumeThresholdMethod());

        // If requestVolumeThreshold is not configured to 4, this would NOT throw a CircuitBreakerOpenException
        expectCbOpen(() -> bean.requestVolumeThresholdMethod());
    }

    @Test
    public void testConfigureFailureRatio() {
        // In annotation: requestVolumeThreshold = 10
        // failureRatio = 1.0
        // In config: failureRatio = 0.8

        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expectNoException(() -> bean.failureRatioMethod(false));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expectNoException(() -> bean.failureRatioMethod(false));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));
        expect(TestConfigExceptionA.class, () -> bean.failureRatioMethod(true));

        // If failureRatio is not configured to 0.8, this would NOT throw a CircuitBreakerOpenException:
        // - if failureRatio > 0.8, this would throw TestConfigExceptionA
        // - if failureRatio < 0.8, CircuitBreakerOpenException would be thrown sooner
        expectCbOpen(() -> bean.failureRatioMethod(false));
    }

    @Test
    public void testConfigureSuccessThreshold() {
        // In annotation: delay = 1000
        // requestVolumeThreshold = 10
        // successThreshold = 4
        // In config: successThreshold = 2

        for (int i = 0; i < 10; i++) {
            expect(TestConfigExceptionA.class, () -> bean.successThresholdMethod(true));
        }

        // CB is now open, wait until it moves to half-open
        expectCbOpen(() -> bean.successThresholdMethod(false));
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            expectNoException(() -> bean.successThresholdMethod(false));
        });

        // CB is now half-open and 1st successful invocation already occured
        expectNoException(() -> bean.successThresholdMethod(false));

        // 2nd successful invocation occured, CB is now closed
        for (int i = 0; i < 10; i++) {
            // 10 because after moving to close, we start with a new, empty rolling window
            expect(TestConfigExceptionA.class, () -> bean.successThresholdMethod(true));
        }

        // CB is now open, wait until it moves to half-open
        expectCbOpen(() -> bean.successThresholdMethod(false));
        await().atMost(1, TimeUnit.MINUTES).untilAsserted(() -> {
            expectNoException(() -> bean.successThresholdMethod(false));
        });

        // CB is now half-open and 1st successful invocation already occured
        expect(TestConfigExceptionA.class, () -> bean.successThresholdMethod(true));

        // 2nd invocation was a failure, CB is back to open
        expectCbOpen(() -> bean.successThresholdMethod(false));
    }

}
