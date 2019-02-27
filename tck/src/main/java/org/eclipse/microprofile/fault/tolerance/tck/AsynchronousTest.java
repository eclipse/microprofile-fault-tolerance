/*
 *******************************************************************************
 * Copyright (c) 2016-2017 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck;


import java.io.File;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common.AsyncBridge;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common.ServiceTask;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.common.Task;
import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.annotations.Test;

import static org.awaitility.Awaitility.await;

/**
 * Verify the asynchronous invocation
 */
public class AsynchronousTest extends Arquillian {

    private @Inject
    AsyncClient client;

    private @Inject
    AsyncClassLevelClient clientClass;

    @Deployment
    public static WebArchive deploy() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
            .resolve("org.awaitility:awaitility", "org.assertj:assertj-core").withTransitivity()
            .asFile();

        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftAsynchronous.jar")
            .addClasses(AsyncClient.class, AsyncClassLevelClient.class, AsyncBridge.class, Task.class,
                Connection.class, ServiceTask.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "ftAsynchronous.war")
            .addAsLibrary(testJar)
            .addAsLibraries(libs);
    }

    /**
     * Test that the future returned by calling an asynchronous method is not done if called right after the operation
     */
    @Test
    public void testAsyncIsNotFinished() {
        Task taskToPerform = new ServiceTask();
        Future<Task> taskResult = client.service(taskToPerform);
        await().untilAsserted(()->  Assertions.
            assertThat(taskResult.isDone()).isFalse());
    }

    /**
     * Test that the future returned by calling an asynchronous method is done if called after waiting enough time to end the operation
     */
    @Test
    public void testAsyncIsFinished() {
        Task taskToPerform = new ServiceTask();
        Future<Task> taskResult = client.service(taskToPerform);
        await().untilAsserted(()->  Assertions.
            assertThat(taskResult.isDone()).isTrue());
    }


    /**
     * Test that the future returned by calling a method in an asynchronous class is not done if called right after the operation
     */
    @Test
    public void testClassLevelAsyncIsNotFinished() {
        Task taskToPerform = new ServiceTask();
        Future<Task> taskResult = clientClass.service(taskToPerform);
        await().untilAsserted(()->  Assertions.
            assertThat(taskResult.isDone()).isFalse());
    }

    /**
     * Test that the future returned by calling a method in an asynchronous class is done if called after waiting enough time to end the operation
     */
    @Test
    public void testClassLevelAsyncIsFinished() {
        Task taskToPerform = new ServiceTask();
        Future<Task> taskResult = clientClass.service(taskToPerform);
        await().untilAsserted(()->  Assertions.
            assertThat(taskResult.isDone()).isTrue());
    }
}
