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

package org.eclipse.microprofile.fault.tolerance.tck.bulkhead;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.nullValue;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.BulkheadPressureBean;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.fault.tolerance.tck.util.TCKConfig;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test that a bulkhead behaves correctly under pressure, in something more resembling a real-world scenario.
 */
public class BulkheadPressureTest extends Arquillian {

    @Deployment
    public static WebArchive deployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadPressure.jar")
                .addPackage(Packages.UTILS)
                .addClass(BulkheadPressureBean.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadPressure.war")
                .addAsLibraries(jar);

        return war;
    }

    private TCKConfig config = TCKConfig.getConfig();

    @Inject
    private AsyncCaller executor;

    @Inject
    private BulkheadPressureBean bean;

    @Test
    public void testBulkheadPressureSync() throws InterruptedException {
        bean.reset();

        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                futures.add(executor.run(() -> bean.servicePressure(config.getTimeoutInMillis(50))));
                Thread.sleep(config.getTimeoutInMillis(25));
            }
        } finally {
            // We want to wait for every started task to finish, even if there was an exception starting one
            await().untilAsserted(() -> futures.forEach(f -> assertTrue(f.isDone())));
        }

        Map<ResultCategory, List<Future<?>>> results =
                futures.stream().collect(Collectors.groupingBy(this::getResultCategory));

        // Bulkhead size is 5, so at least 5 tasks should complete
        assertThat("Calls returning successfully", results.get(ResultCategory.NO_EXCEPTION),
                hasSize(greaterThanOrEqualTo(5)));
        // Everything should either complete or fail with a bulkhead exception
        assertThat("Calls throwing non-bulkhead exception", results.get(ResultCategory.OTHER_EXCEPTION),
                is(nullValue()));

        assertThat("Max concurrent tasks", bean.getMaxInProgress(), lessThanOrEqualTo(5));
    }

    @Test
    public void testBulkheadPressureAsync() throws InterruptedException {
        bean.reset();

        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 100; i++) {
                futures.add(bean.servicePressureAsync(config.getTimeoutInMillis(50)));
                Thread.sleep(config.getTimeoutInMillis(25));
            }
        } finally {
            // We want to wait for every started task to finish, even if there was an exception starting one
            await().untilAsserted(() -> futures.forEach(f -> assertTrue(f.isDone())));
        }

        Map<ResultCategory, List<Future<?>>> results =
                futures.stream().collect(Collectors.groupingBy(this::getResultCategory));

        // Bulkhead size is 5 and queue size is 5, so at least 10 tasks should complete
        assertThat("Calls returning successfully", results.get(ResultCategory.NO_EXCEPTION),
                hasSize(greaterThanOrEqualTo(10)));
        // Everything should either complete or fail with a bulkhead exception
        assertThat("Calls throwing non-bulkhead exception", results.get(ResultCategory.OTHER_EXCEPTION),
                is(nullValue()));

        assertThat("Max concurrent tasks", bean.getMaxInProgress(), lessThanOrEqualTo(5));
    }

    private enum ResultCategory {
        BULKHEAD_EXCEPTION, OTHER_EXCEPTION, NO_EXCEPTION
    }

    private ResultCategory getResultCategory(Future<?> future) {
        assertTrue(future.isDone(), "Checking result category when future is not done");
        try {
            future.get();
            return ResultCategory.NO_EXCEPTION;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof BulkheadException) {
                return ResultCategory.BULKHEAD_EXCEPTION;
            } else {
                return ResultCategory.OTHER_EXCEPTION;
            }
        } catch (InterruptedException e) {
            throw new AssertionError("Interrupted getting result category", e);
        }
    }

}
