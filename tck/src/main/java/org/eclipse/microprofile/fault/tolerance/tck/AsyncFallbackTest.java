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
package org.eclipse.microprofile.fault.tolerance.tck;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.fallback.AsyncFallbackClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Test the combination of the @Asynchronous and @Fallback annotations.
 */
public class AsyncFallbackTest extends Arquillian {
    @Inject
    private AsyncFallbackClient client;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftAsyncFallback.jar")
                .addClasses(AsyncFallbackClient.class, CompletableFutureHelper.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftAsyncFallback.war")
                .addAsLibrary(testJar);
        return war;
    }

    @Test
    public void testAsyncFallbackSuccess() throws ExecutionException, InterruptedException {
        assertThat("Future-returning method that returns successfully should not fallback",
                client.service1().get(), equalTo("Success"));
    }

    @Test
    public void testAsyncFallbackMethodThrows() throws IOException, ExecutionException, InterruptedException {
        assertThat("Future-returning method that throws an exception should fallback",
                client.service2().get(), equalTo("Fallback"));
    }

    @Test
    public void testAsyncFallbackFutureCompletesExceptionally() throws InterruptedException {
        try {
            client.service3().get();
            fail("ExecutionException not thrown");
        } catch (ExecutionException expected) {
            assertThat("Future-returning method that returns failing future should not fallback",
                    expected.getCause(), instanceOf(IOException.class));
        }
    }

    @Test
    public void testAsyncCSFallbackSuccess() throws ExecutionException, InterruptedException {
        assertThat("CompletionStage-returning method that returns successfully should not fallback",
                CompletableFutureHelper.toCompletableFuture(client.serviceCS1()).get(), equalTo("Success"));
    }

    @Test
    public void testAsyncCSFallbackMethodThrows() throws IOException, ExecutionException, InterruptedException {
        assertThat("CompletionStage-returning method that throws an exception should fallback",
                CompletableFutureHelper.toCompletableFuture(client.serviceCS2()).get(), equalTo("Fallback"));
    }

    @Test
    public void testAsyncCSFallbackFutureCompletesExceptionally() throws InterruptedException, ExecutionException {
        assertThat("CompletionStage-returning method that returns failing CompletionStage should fallback",
                CompletableFutureHelper.toCompletableFuture(client.serviceCS3()).get(), equalTo("Fallback"));
    }
}
