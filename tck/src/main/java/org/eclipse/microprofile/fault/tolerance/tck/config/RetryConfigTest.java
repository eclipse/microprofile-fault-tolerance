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
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertEquals;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that the various parameters of Retry can be configured
 */
public class RetryConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .set(RetryConfigBean.class, "serviceMaxRetries", Retry.class, "maxRetries", "10")
                .set(RetryConfigBean.class, "serviceMaxDuration", Retry.class, "maxDuration", "1")
                .set(RetryConfigBean.class, "serviceMaxDuration", Retry.class, "durationUnit", "SECONDS")
                .set(RetryConfigBean.class, "serviceDelay", Retry.class, "delay", "2000")
                .set(RetryConfigBean.class, "serviceDelay", Retry.class, "delayUnit", "MICROS")
                .set(RetryConfigBean.class, "serviceRetryOn", Retry.class, "retryOn",
                        TestConfigExceptionA.class.getName() + "," + TestConfigExceptionB.class.getName())
                .set(RetryConfigBean.class, "serviceAbortOn", Retry.class, "abortOn",
                        TestConfigExceptionA.class.getName() + "," + TestConfigExceptionB1.class.getName())
                .set(RetryConfigBean.class, "serviceJitter", Retry.class, "jitter", "1")
                .set(RetryConfigBean.class, "serviceJitter", Retry.class, "jitterDelayUnit", "SECONDS");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftRetryConfig.jar")
                .addClasses(RetryConfigBean.class)
                .addClasses(TestConfigExceptionA.class, TestConfigExceptionB.class, TestConfigExceptionB1.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(config, "microprofile-config.properties");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftRetryConfig.war")
                .addAsLibrary(jar);

        return war;
    }

    @Inject
    private RetryConfigBean bean;

    @Test
    public void testConfigMaxRetries() {
        // In annotation: maxRetries not set (default 3)
        // In config: maxRetries = 10
        AtomicInteger counter = new AtomicInteger();
        expect(TestException.class, () -> bean.serviceMaxRetries(counter));

        assertEquals(counter.get(), 11);
    }

    @Test
    public void testConfigMaxDuration() {
        // In annotation: maxDuration = 10000ms (10s)
        // maxRetries = 10000
        // delay = 200ms
        // In config: maxDuration = 1s

        long startTime = System.nanoTime();
        expect(TestException.class, () -> bean.serviceMaxDuration());
        long endTime = System.nanoTime();

        Duration duration = Duration.ofNanos(endTime - startTime);

        // Expected time without config: 10s
        // Expected time with config: 1s
        assertThat(duration, lessThan(Duration.ofSeconds(8)));
    }

    @Test
    public void testConfigDelay() {
        // In annotation: delay = 2s (2000ms)
        // In config: delay = 2000µs (2ms)

        long startTime = System.nanoTime();
        expect(TestException.class, () -> bean.serviceDelay());
        long endTime = System.nanoTime();

        Duration duration = Duration.ofNanos(endTime - startTime);

        // Expected time without config: 2s * 5 retries -> 10s
        // Expected time with config: 2ms * 5 retries -> 10ms
        assertThat(duration, lessThan(Duration.ofSeconds(8)));
    }

    @Test
    public void testConfigRetryOn() {
        // In annotation: retryOn not set (default Exception)
        // maxRetries = 1
        // In config: retryOn = TestConfigExceptionA, TestConfigExceptionB

        AtomicInteger counter = new AtomicInteger();

        counter.set(0);
        expect(TestException.class, () -> bean.serviceRetryOn(new TestException(), counter));
        assertEquals(counter.get(), 1); // Not retried

        counter.set(0);
        expect(TestConfigExceptionA.class, () -> bean.serviceRetryOn(new TestConfigExceptionA(), counter));
        assertEquals(counter.get(), 2); // Retried

        counter.set(0);
        expect(TestConfigExceptionB.class, () -> bean.serviceRetryOn(new TestConfigExceptionB(), counter));
        assertEquals(counter.get(), 2); // Retried

        counter.set(0);
        expect(TestConfigExceptionB1.class, () -> bean.serviceRetryOn(new TestConfigExceptionB1(), counter));
        assertEquals(counter.get(), 2); // Retried
    }

    @Test
    public void testConfigAbortOn() {
        // In annotation: retryOn = TestConfigExceptionA, TestConfigExceptionB
        // abortOn = RuntimeException
        // maxRetries = 1
        // In config: abortOn = TestConfigExceptionA, TestConfigExceptionB1

        AtomicInteger counter = new AtomicInteger();

        counter.set(0);
        expect(TestException.class, () -> bean.serviceAbortOn(new TestException(), counter));
        assertEquals(counter.get(), 1); // Not retried

        counter.set(0);
        expect(TestConfigExceptionA.class, () -> bean.serviceAbortOn(new TestConfigExceptionA(), counter));
        assertEquals(counter.get(), 1); // Not retried

        counter.set(0);
        expect(TestConfigExceptionB.class, () -> bean.serviceAbortOn(new TestConfigExceptionB(), counter));
        assertEquals(counter.get(), 2); // Retried

        counter.set(0);
        expect(TestConfigExceptionB1.class, () -> bean.serviceAbortOn(new TestConfigExceptionB1(), counter));
        assertEquals(counter.get(), 1); // Not retried
    }

    @Test
    public void testConfigJitter() {
        // In annotation: jitter = 0ms
        // delay = 0ms
        // In config: jitter = 1s

        // serviceJitter() will throw TestConfigExceptionA if a delay of > 100ms is observed
        expect(TestConfigExceptionA.class, () -> bean.serviceJitter());

        // Note: it's possible for this test to pass incorrectly if an external factor causes a delay
        // As jitter is random, it's technically possible but very unlikely for this test to fail for a correct
        // implementation
    }

}
