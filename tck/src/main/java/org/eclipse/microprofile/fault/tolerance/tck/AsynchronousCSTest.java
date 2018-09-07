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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper;
import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.Assert.ThrowingRunnable;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

/**
 * Verify the asynchronous invocation with COmpletionStage
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
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftAsynchronous.jar")
                .addClasses(AsyncClient.class, AsyncClassLevelClient.class, Connection.class, CompletableFutureHelper.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftAsynchronous.war").addAsLibrary(testJar);
        return war;
    }

    /**
     * Test that the stage returned by calling an asynchronous method is not
     * done if called right after the operation
     */
    @Test
    public void testAsyncIsNotFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        CompletionStage<Connection> resultFuture = client.serviceCS(waitingFuture);

        Assert.assertFalse(completesQuickly(resultFuture));

        complete(waitingFuture);

        try {
            waitUntilCompleted(resultFuture);
        }
        catch (CompletionException e) {
            handleCompletionException(e);
        }
    }

    /**
     * Test that the stage returned by calling an asynchronous method is done
     * if called after waiting enough time to end the operation
     */
    @Test
    public void testAsyncIsFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        CompletionStage<Connection> resultFuture = client.serviceCS(waitingFuture);
        complete(waitingFuture);

        Assert.assertTrue(completesQuickly(resultFuture));


        try {
            waitUntilCompleted(resultFuture);
        }
        catch (CompletionException e) {
            handleCompletionException(e);
        }
    }

    /**
     * Test that the stage returned by calling a method in an asynchronous
     * class is not done if called right after the operation
     */
    @Test
    public void testClassLevelAsyncIsNotFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        CompletionStage<Connection> resultFuture = clientClass.serviceCS(waitingFuture);

        Assert.assertFalse(completesQuickly(resultFuture));

        complete(waitingFuture);

        try {
            waitUntilCompleted(resultFuture);
        }
        catch (CompletionException e) {
            handleCompletionException(e);
        }
    }

    /**
     * Test that the stage returned by calling a method in an asynchronous
     * class is done if called after waiting enough time to end the operation
     */
    @Test
    public void testClassLevelAsyncIsFinished() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        CompletionStage<Connection> resultFuture = clientClass.serviceCS(waitingFuture);
        complete(waitingFuture);

        Assert.assertTrue(completesQuickly(resultFuture));


        try {
            waitUntilCompleted(resultFuture);
        }
        catch (CompletionException e) {
            handleCompletionException(e);
        }
    }

    /**
     * Test that the callbacks added to the initial stage are executed
     * after the stage returned by the asynchronous method call is completed.
     * 
     * The callbacks added inside method invocation must be called first and 
     * then callbacks added to the result of the call (on the calling thread)
     * must be executed in the order they were added.
     */
    @Test
    public void testAsyncCallbacksChained() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        StringBuilder executionRecord = new StringBuilder();
        
        CompletableFuture<Connection> innerFuture = new CompletableFuture<Connection>();
        CompletableFuture<Connection> returnedFuture = innerFuture
                .thenApply(v -> {
                    executionRecord.append("1");
                    return v;
                });
        CompletionStage<Connection> resultFuture = client
                .serviceCS(waitingFuture, returnedFuture)
                .thenApply(v -> {
                    executionRecord.append("2");
                    return v;
                });
        complete(waitingFuture);
        resultFuture = resultFuture.thenApply(v -> {
                    executionRecord.append("3");
                    return v;
                });

        Assert.assertFalse(completesQuickly(resultFuture), 
                "Stage returned by the method isn't completed yet so also the outer stage mustn't be completed");
        innerFuture.complete(new EmptyConnection());
        Assert.assertTrue(completesQuickly(resultFuture),
                "Stage returned by the method is completed so also the outer stage must be completed");
        Assert.assertEquals(executionRecord.toString(), "123", 
                "The execution didn't happen in the expected order");

        try {
            waitUntilCompleted(resultFuture);
        }
        catch (CompletionException e) {
            handleCompletionException(e);
        }
    }

    /**
     * Test that the stage returned by calling an asynchronous method is 
     * completed exceptionally if the method throws an exception
     */
    @Test
    public void testAsyncCompletesExceptionallyWhenExceptionThrown() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        CompletionStage<Connection> resultFuture = client.serviceCS(waitingFuture, true);
        waitingFuture.completeExceptionally(new SimulatedException("completedExceptionally"));

        Assert.assertTrue(completesQuickly(resultFuture));
        Assert.assertTrue(isCompletedExceptionally(resultFuture));
        Assert.assertFalse(isCancelled(resultFuture));
        assertThrowsExecutionExceptionWithCause(SimulatedException.class, CompletableFutureHelper.toCompletableFuture(resultFuture)::get);
    }

    /**
     * Test that the stage returned by calling an asynchronous method is 
     * completed exceptionally if the method returns a stage completed exceptionally
     */
    @Test
    public void testAsyncCompletesExceptionallyWhenCompletedExceptionally() {
        CompletableFuture<Void> waitingFuture = newWaitingFuture();
        
        CompletionStage<Connection> resultFuture = client.serviceCS(waitingFuture, false);
        waitingFuture.completeExceptionally(new SimulatedException("completedExceptionally"));

        Assert.assertTrue(completesQuickly(resultFuture));
        Assert.assertTrue(isCompletedExceptionally(resultFuture));
        Assert.assertFalse(isCancelled(resultFuture));
        assertThrowsExecutionExceptionWithCause(SimulatedException.class, CompletableFutureHelper.toCompletableFuture(resultFuture)::get);
    }

    /**
     * Ensure that any waiting futures get completed at the end of each test
     * <p>
     * Important in case tests end early due to an exception or failure.
     */
    @AfterTest
    public void completeWaitingFutures() {
        waitingFutures.forEach((future) -> {
            future.complete(null);
        });
        waitingFutures.clear();
    }

    /**
     * Use this method to obtain futures for passing to methods on
     * {@link AsyncClient}
     * <p>
     * Using this factory method ensures they will be completed at the end of
     * the test if your test fails.
     */
    private CompletableFuture<Void> newWaitingFuture() {
        CompletableFuture<Void> result = new CompletableFuture<>();
        waitingFutures.add(result);
        return result;
    }

    /**
     * A helper method to complete a waiting future with a more readable syntax
     */
    private void complete(CompletableFuture<?> future) {
        future.complete(null);
    }

    private void handleCompletionException(CompletionException e) throws AssertionError {
        throw new AssertionError("testAsync: unexpected Exception calling service: "
                + e.getCause().getMessage(), e);
    }
    
    private static Connection waitUntilCompleted(CompletionStage<Connection> resultFuture) {
        return CompletableFutureHelper.toCompletableFuture(resultFuture).join();
    }
    
    /**
     * Tests whether the given CompletionStage completes within 500ms
     * <p>
     * Used to avoid a race condition where a test wants to take some action which
     * should cause the CompletionStage to complete, but asynchronous execution
     * means it may not happen immediately.
     */
    private static boolean completesQuickly(CompletionStage<Connection> resultFuture) {
        try {
            CompletableFutureHelper.toCompletableFuture(resultFuture).get(500, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ex) {
            // Did not complete quickly
            return false;
        }
        catch (Exception ex) {
            // Completed with exception, fall through, we don't care about the result
        }
        return true;
    }

    private static boolean isCancelled(CompletionStage<Connection> resultFuture) {
        return CompletableFutureHelper.toCompletableFuture(resultFuture).isCancelled();
    }

    private static boolean isCompletedExceptionally(CompletionStage<Connection> resultFuture) {
        return CompletableFutureHelper.toCompletableFuture(resultFuture).isCompletedExceptionally();
    }
    
    private static void assertThrowsExecutionExceptionWithCause(Class<? extends Throwable> causeClazz, ThrowingRunnable runnable) {
        try {
            runnable.run();
            Assert.fail("ExecutionException not thrown");
        }
        catch (ExecutionException ex) {
            Assert.assertTrue(causeClazz.isInstance(ex.getCause()), "Cause of ExecutionException was " + ex.getCause());
        }
        catch (Throwable ex) {
            Assert.fail("Unexpected exception thrown", ex);
        }
    }

    private static class SimulatedException extends RuntimeException {

        public SimulatedException() {
        }

        public SimulatedException(String message) {
            super(message);
        }
        
    }
    
    private static class EmptyConnection implements Connection {

        @Override
        public String getData() {
            return null;
        }
        
    }

}
