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
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.with;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.endsWith;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

public class AsyncClientTest  extends Arquillian {

    @Inject
    private AsyncClient client;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftAsynchronousAwaitPOC.jar")
            .addClasses(AsyncClient.class, AsyncBridge.class, Task.class, FakeServiceTask.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftAsynchronousAwaitPOC.war").addAsLibrary(testJar);
        return war;
    }

    @Test(timeOut = 2000)
    public void awaitAssertJAssertionAsLambda(){
        Task taskToPerform = new FakeServiceTask();
         Future<Task> taskResult = client.service(taskToPerform);
        await().untilAsserted(()->  Assertions.
            assertThat(taskToPerform.getTaskResult()).isEqualTo("Task Done!!"));

         assertTrue(taskResult.isDone());

    }

    @Test(timeOut = 2000)
    public void awaitJUnitAssertionDisplaysOriginalErrorMessageAndTimeoutWhenConditionTimeoutExceptionOccurs() {
        Task taskToPerform = new FakeServiceTask();
        CompletionStage<Task> taskResult = client.serviceCS(taskToPerform);
        exception.expect(ConditionTimeoutException.class);
        exception.expectMessage(startsWith("Assertion condition defined as a lambda expression in " + AsyncClientTest.class.getName()));
        exception.expectMessage(endsWith("expected:<Task Done!!> but was:<null> within 120 milliseconds."));

        with().pollInterval(10, MILLISECONDS).then().await().atMost(120, MILLISECONDS).untilAsserted(
            () -> assertEquals("Task Done!!", taskToPerform.getTaskResult()));

        assertTrue(taskResult.toCompletableFuture().completeExceptionally(new Exception("Async task fails")));
    }
}
