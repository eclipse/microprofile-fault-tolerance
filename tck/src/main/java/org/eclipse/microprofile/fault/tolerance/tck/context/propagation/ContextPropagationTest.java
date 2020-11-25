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
package org.eclipse.microprofile.fault.tolerance.tck.context.propagation;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.SkipException;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.eclipse.microprofile.fault.tolerance.tck.asynchronous.CompletableFutureHelper.toCompletableFuture;
import static org.testng.Assert.assertEquals;

public class ContextPropagationTest extends Arquillian {
    // configure identical defaults for both ManagedExecutor and ThreadContext, because implementations
    // are free to use any of them to implement context propagation
    private static final String CONTEXT_PROPAGATION_CONFIG = ""
        + "mp.context.ManagedExecutor.propagated=CDI,MyContext1\n"
        + "mp.context.ManagedExecutor.cleared=MyContext2\n"
        + "mp.context.ThreadContext.propagated=CDI,MyContext1\n"
        + "mp.context.ThreadContext.cleared=MyContext2\n";

    @Deployment
    public static WebArchive deploy() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "ftContextPropagation.jar")
            .addClasses(MyAppScopedBean.class, MyReqScopedBean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");


        if (contextPropagationPresent()) {
            // if this was always added, deployment would fail at runtime
            // when MP Context Propagation classes are not present

            jar.addClasses(MyContext1.class, MyContext1Provider.class, MyContext2.class, MyContext2Provider.class)
                .addAsServiceProvider(ThreadContextProvider.class, MyContext1Provider.class, MyContext2Provider.class)
                .addAsManifestResource(new StringAsset(CONTEXT_PROPAGATION_CONFIG), "microprofile-config.properties");

        }

        return ShrinkWrap.create(WebArchive.class, "ftContextPropagation.war")
            .addAsLibrary(jar);
    }

    @Inject
    private MyAppScopedBean appScopedBean;

    @Inject
    private MyReqScopedBean reqScopedBean;

    @Test
    public void contextPropagation() throws InterruptedException, ExecutionException, TimeoutException {
        assumeContextPropagationPresence();

        MyContext1.set("foo");
        MyContext2.set("bar");
        reqScopedBean.set("quux");

        assertEquals(MyContext1.get(), "foo");
        assertEquals(MyContext2.get(), "bar");
        assertEquals(reqScopedBean.get(), "quux");

        // in the @Asynchronous method, CDI should be propagated, MyContext1 should be propagated,
        // and MyContext2 should be cleared
        String value = toCompletableFuture(appScopedBean.get()).get(1, TimeUnit.MINUTES);
        assertEquals(value, "foo|[[NONE]]|quux");

        assertEquals(MyContext1.get(), "foo");
        assertEquals(MyContext2.get(), "bar");
        assertEquals(reqScopedBean.get(), "quux");
    }

    private static void assumeContextPropagationPresence() {
        if (!contextPropagationPresent()) {
            throw new SkipException("Context Propagation not present, ignoring test");
        }
    }

    private static boolean contextPropagationPresent() {
        try {
            Class.forName("org.eclipse.microprofile.context.ThreadContext");
            return true;
        }
        catch (ClassNotFoundException e) {
            return false;
        }
    }
}
