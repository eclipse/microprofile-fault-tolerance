/*
 *******************************************************************************
 * Copyright (c) 2016-2018 Contributors to the Eclipse Foundation
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
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
import static org.awaitility.Awaitility.with;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


/**
 * Verify the asynchronous invocation with {@link CompletionStage}
 *
 * @author Ondro Mihalyi
 */
public class AsynchronousCSTest extends Arquillian {

    private @Inject
    AsyncClient client;

    private @Inject
    AsyncClassLevelClient clientClass;

    private List<CompletableFuture<Void>> waitingFutures = new ArrayList<>();

    @Deployment
    public static WebArchive deploy() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
            .resolve("org.awaitility:awaitility", "org.assertj:assertj-core").withTransitivity()
            .asFile();

        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftAsynchronous.jar")
            .addClasses(AsyncClient.class, AsyncClassLevelClient.class, Connection.class, CompletableFutureHelper.class,
                AsyncBridge.class, Task.class, ServiceTask.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "ftAsynchronous.war")
            .addAsLibrary(testJar)
            .addAsLibraries(libs);
    }

    /**
     * Test that the stage returned by calling an asynchronous method is not
     * done if called right after the operation
     */
    @Test
    public void testAsyncIsNotFinished() {
        Task taskToPerform = new ServiceTask();
        CompletionStage<Task> taskResult = client.serviceCS(taskToPerform);
        assertFalse(taskResult.toCompletableFuture().isDone());
    }

    /**
     * Test that the stage returned by calling an asynchronous method is done
     * if called after waiting enough time to end the operation
     */
    @Test
    public void testAsyncIsFinished() {
        Task taskToPerform = new ServiceTask();
        CompletionStage<Task> taskResult = client.serviceCS(taskToPerform);
        await().untilAsserted(() -> Assertions.
            assertThat(taskResult.toCompletableFuture().isDone()).isTrue());
    }

    /**
     * Test that the stage returned by calling a method in an asynchronous
     * class is not done if called right after the operation
     */
    @Test
    public void testClassLevelAsyncIsNotFinished() {
        Task taskToPerform = new ServiceTask();
        CompletionStage<Task> taskResult = clientClass.serviceCS(taskToPerform);
        assertFalse(taskResult.toCompletableFuture().isDone());
    }

    /**
     * Test that the stage returned by calling a method in an asynchronous
     * class is done if called after waiting enough time to end the operation
     */
    @Test
    public void testClassLevelAsyncIsFinished() {
        Task taskToPerform = new ServiceTask();
        CompletionStage<Task> taskResult = clientClass.serviceCS(taskToPerform);
        await().untilAsserted(() -> Assertions.
            assertThat(taskResult.toCompletableFuture().isDone()).isTrue());
    }

    /**
     * Test that the callbacks added to the initial stage are executed
     * after the stage returned by the asynchronous method call is completed.
     * <p>
     * The callbacks added inside method invocation must be called first and
     * then callbacks added to the result of the call (on the calling thread)
     * must be executed in the order they were added.
     */
    @Test
    public void testAsyncCallbacksChained() {
        StringBuilder executionRecord = new StringBuilder();
        Task taskToPerform = new ServiceTask();
        CompletionStage<Task> taskResult = client.serviceCS(taskToPerform).thenApply(v -> {
            executionRecord.append("1");
            return v;
        }).thenApply(v -> {
            executionRecord.append("2");
            return v;
        }).thenApply(v -> {
            executionRecord.append("3");
            return v;
        });

        await().untilAsserted(() -> Assertions.
            assertThat(taskResult.toCompletableFuture().isDone()).isTrue());

        assertEquals(executionRecord.toString(), "123");
    }

    /**
     * Test that the stage returned by calling an asynchronous method is
     * completed exceptionally if the method throws an exception
     */
    @Test
    public void testAsyncCompletesExceptionallyWhenExceptionThrown() {
        Task taskToPerform = new ServiceTask();
        CompletionStage<Task> taskResult = client.serviceCS(taskToPerform);
        try {
            with().pollThread(Thread::new).await()
                .atMost(120, TimeUnit.MILLISECONDS).until(
                () -> taskToPerform.getTaskResult() == "service DATA");

            assertTrue(taskResult.toCompletableFuture().isCompletedExceptionally());

            fail("Should throw ConditionTimeoutException");
        }
        catch (ConditionTimeoutException e) {
            assertTrue(taskResult.toCompletableFuture().isDone());
        }
    }
}
