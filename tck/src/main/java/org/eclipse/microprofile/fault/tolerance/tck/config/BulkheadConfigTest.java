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

import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager;
import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncTaskManager.BarrierTask;
import org.eclipse.microprofile.fault.tolerance.tck.util.Packages;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
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
 * Test that the parameters of Bulkhead can be configured
 */
public class BulkheadConfigTest extends Arquillian {

    @Deployment
    public static WebArchive create() {
        ConfigAnnotationAsset config = new ConfigAnnotationAsset()
                .set(BulkheadConfigBean.class, "serviceValue", Bulkhead.class, "value", "1")
                .set(BulkheadConfigBean.class, "serviceWaitingTaskQueue", Bulkhead.class, "waitingTaskQueue", "1");

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftBulkheadConfig.jar")
                .addClass(BulkheadConfigBean.class)
                .addPackage(Packages.UTILS)
                .addAsManifestResource(config, "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftBulkheadConfig.war")
                .addAsLibrary(jar);

        return war;
    }

    @Inject
    private BulkheadConfigBean bean;

    @Test
    public void testConfigValue() throws Exception {
        // In annotation: value = 5
        // In config: value = 1

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<Void> taskA = taskManager.runBarrierTask(bean::serviceValue);
            taskA.assertAwaits();

            BarrierTask<Void> taskB = taskManager.runBarrierTask(bean::serviceValue);
            taskB.assertThrows(BulkheadException.class);
        }
    }

    @Test
    public void testWaitingTaskQueue() throws Exception {
        // In annotation: waitingTaskQueue = 5
        // value = 1
        // In config: waitingTaskQueue = 1

        try (AsyncTaskManager taskManager = new AsyncTaskManager()) {
            BarrierTask<Void> taskA = taskManager.runAsyncBarrierTask(bean::serviceWaitingTaskQueue);
            taskA.assertAwaits();

            BarrierTask<Void> taskB = taskManager.runAsyncBarrierTask(bean::serviceWaitingTaskQueue);
            taskB.assertNotAwaiting();

            BarrierTask<Void> taskC = taskManager.runAsyncBarrierTask(bean::serviceWaitingTaskQueue);
            taskC.assertThrows(BulkheadException.class);
        }
    }
}
