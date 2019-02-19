/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.awaitility.poc;

import org.assertj.core.api.Assertions;
import org.awaitility.core.ConditionTimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.testng.Assert.fail;


import java.io.File;
//import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

public class AsyncClientTest  extends Arquillian {

    @Inject
    private AsyncClient client;

    @Deployment
    public static WebArchive deploy() {
        File[] libs = Maven.resolver().loadPomFromFile("pom.xml")
            .resolve("org.awaitility:awaitility", "org.assertj:assertj-core").withTransitivity()
            .asFile();


        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftAsynchronousAwaitPOC.jar")
            .addClasses(AsyncClient.class, AsyncBridge.class, Task.class, FakeServiceTask.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftAsynchronousAwaitPOC.war")
            .addAsLibrary(testJar)
            .addAsLibraries(libs);
        return war;
    }

    @Test
    public void awaitAssertJAssertionAsLambda(){
        Task taskToPerform = new FakeServiceTask();
         Future<Task> taskResult = client.service(taskToPerform);
        await().untilAsserted(()->  Assertions.
            assertThat(taskToPerform.getTaskResult()).isEqualTo("Task Done!!"));

         assertTrue(taskResult.isDone());

    }

    @Test(expectedExceptions = ConditionTimeoutException.class )
    public void awaitAssertionTimeoutWhenConditionTimeoutExceptionOccurs() {
        Task taskToPerform = new FakeServiceTask();
        client.serviceCS(taskToPerform);

        with().pollInterval(10, MILLISECONDS).then().await().atMost(120, MILLISECONDS).untilAsserted(
            () -> assertEquals(taskToPerform.getTaskResult(), "Task Done!!"));
    }

    @Test
    public void awaitAssertionTimeoutWhenConditionTimeoutExceptionOccursWithExceptionHandling() {
        Task taskToPerform = new FakeServiceTask();
        CompletionStage<Task> taskResult = client.serviceCS(taskToPerform);

        try {
            with().pollInterval(10, MILLISECONDS).then().await("my alias").atMost(120, MILLISECONDS).untilAsserted(
                () -> assertEquals(taskToPerform.getTaskResult(), "Task Done!!"));
            fail("Should throw ConditionTimeoutException");
        }
        catch (ConditionTimeoutException e) {
            assertThat(countOfOccurrences(e.getMessage(), "my alias")).isEqualTo(1);
            CompletableFuture<Task>taskFuture = taskResult.toCompletableFuture();
            assertEquals(taskToPerform.getTaskResult(), null);
            assertTrue(taskFuture.isDone());
        }

    }

    private static int countOfOccurrences(String str, String subStr) {
        return (str.length() - str.replaceAll(Pattern.quote(subStr), "").length()) / subStr.length();
    }
}
