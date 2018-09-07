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


import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.asynchronous.AsyncClient;
import org.eclipse.microprofile.fault.tolerance.tck.util.Connection;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

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
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftAsynchronous.jar")
            .addClasses(AsyncClient.class, AsyncClassLevelClient.class, Connection.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "ftAsynchronous.war").addAsLibrary(testJar);
        return war;
    }


    /**
     * Test that the future returned by calling an asynchronous method is not done if called right after the operation
     */
    @Test
    public void testAsyncIsNotFinished() {
        Future<Connection> future = null;
        try {
            future = client.service();
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsync: unexpected InterruptedException calling service");
        }

        Assert.assertFalse(future.isDone());

    }

    /**
     * Test that the future returned by calling an asynchronous method is done if called after waiting enough time to end the operation
     */
    @Test
    public void testAsyncIsFinished() {
        Future<Connection> future = null;
        try {
            future = client.service();
            Thread.sleep(1500);
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsync: unexpected InterruptedException calling service");
        }

        Assert.assertTrue(future.isDone());

    }


    /**
     * Test that the future returned by calling a method in an asynchronous class is not done if called right after the operation
     */
    @Test
    public void testClassLevelAsyncIsNotFinished() {
        Future<Connection> future = null;
        try {
            future = clientClass.service();
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsync: unexpected InterruptedException calling service");
        }

        Assert.assertFalse(future.isDone());

    }

    /**
     * Test that the future returned by calling a method in an asynchronous class is done if called after waiting enough time to end the operation
     */
    @Test
    public void testClassLevelAsyncIsFinished() {
        Future<Connection> future = null;
        try {
            future = clientClass.service();
            Thread.sleep(1500);
        }
        catch (InterruptedException e) {
            throw new AssertionError("testAsync: unexpected InterruptedException calling service");
        }

        Assert.assertTrue(future.isDone());

    }
}
