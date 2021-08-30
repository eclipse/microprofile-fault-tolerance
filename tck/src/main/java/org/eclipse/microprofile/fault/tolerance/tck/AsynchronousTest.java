/*
 *******************************************************************************
 * Copyright (c) 2016-2020 Contributors to the Eclipse Foundation
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

import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncApplicationScopeClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncRequestScopeClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import jakarta.inject.Inject;

/**
 * Verify the asynchronous invocation
 */
public class AsynchronousTest extends Arquillian {

    private @Inject AsyncClient client;

    private @Inject AsyncClassLevelClient clientClass;

    // AsyncApplicationScopeClient is an @ApplicationScoped bean
    private @Inject AsyncApplicationScopeClient clientApplicationScope;

    private List<CompletableFuture<Void>> waitingFutures = new ArrayList<>();

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftAsynchronous.jar")
                .addClasses(AsyncClient.class, AsyncClassLevelClient.class, AsyncApplicationScopeClient.class,
                        AsyncRequestScopeClient.class, Connection.class, CompletableFutureHelper.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "ftAsynchronous.war").addAsLibrary(testJar);
    }

    /**
     * Test that the future returned by calling an asynchronous method is not done if called right after the operation
     */
    @Test
    public void testAsyncIsNotFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        Future<Connection> future = client.service(waitingFuture);
        Assert.assertFalse(future.isDone());
    }

    /**
     * Test that the future returned by calling an asynchronous method is done if called after waiting enough time to
     * end the operation
     */
    @Test
    public void testAsyncIsFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        Future<Connection> future = client.service(waitingFuture);
        waitingFuture.complete(null);
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertTrue(future.isDone()));
    }

    /**
     * Test that the future returned by calling a method in an asynchronous class is not done if called right after the
     * operation
     */
    @Test
    public void testClassLevelAsyncIsNotFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        Future<Connection> future = clientClass.service(waitingFuture);
        Assert.assertFalse(future.isDone());
    }

    /**
     * Test that the future returned by calling a method in an asynchronous class is done if called after waiting enough
     * time to end the operation
     */
    @Test
    public void testClassLevelAsyncIsFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        Future<Connection> future = clientClass.service(waitingFuture);
        waitingFuture.complete(null);
        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> Assert.assertTrue(future.isDone()));
    }

    /**
     * Test that the request context is active during execution for an asynchronous method that returns a
     * CompletionStage
     * 
     * If the request scope is active, then an @ApplicationScoped bean should be able to asynchronously call
     * an @Asynchronous method returning a CompletionStage on a @RequestScoped bean, and return the correct result
     * 
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testAsyncRequestContextWithCompletionStage()
            throws InterruptedException, ExecutionException, TimeoutException {
        CompletionStage<String> completionStage = clientApplicationScope.serviceCallingCompletionStageMethod();
        String result = CompletableFutureHelper.toCompletableFuture(completionStage).get(30, TimeUnit.SECONDS);
        Assert.assertEquals(result, "testCompletionStageString");
    }

    /**
     * Test that the request context is active during execution for an asynchronous method that returns a Future
     * 
     * If the request scope is active, then an @ApplicationScoped bean should be able to asynchronously call
     * an @Asynchronous method returning a Future on a @RequestScoped bean, and return the correct result
     * 
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testAsyncRequestContextWithFuture() throws InterruptedException, ExecutionException, TimeoutException {
        Future<String> future = clientApplicationScope.serviceCallingFutureMethod();
        String result = future.get(30, TimeUnit.SECONDS);
        Assert.assertEquals(result, "testFutureString");
    }

    /**
     * Use this method to obtain futures for passing to methods on {@link AsyncClient}
     * <p>
     * Using this factory method ensures they will be completed at the end of the test if your test fails.
     */
    private CompletableFuture<Void> newWaitingFuture() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        waitingFutures.add(result);
        return result;
    }

    /**
     * Ensure that any waiting futures get completed at the end of each test
     * <p>
     * Important in case tests end early due to an exception or failure.
     */
    @AfterMethod
    public void completeWaitingFutures() {
        waitingFutures.forEach((future) -> {
            future.complete(null);
        });
        waitingFutures.clear();
    }
}
