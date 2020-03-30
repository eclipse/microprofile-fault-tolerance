/*
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
 */

package org.eclipse.microprofile.fault.tolerance.tck.config.retry;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.config.ConfigAnnotationAsset;
import org.eclipse.microprofile.fault.tolerance.tck.config.TestConfigExceptionA;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Antoine Sabot-Durand
 */
public class ConfigPropertyGlobalVsClassVsMethodTest extends Arquillian {

    @Deployment
    public static WebArchive deployAnotherApp() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftConfig.jar")
            .addClasses(BeanWithRetry.class, TestConfigExceptionA.class, CustomRetryServiceException.class)
            .addAsManifestResource(
                new ConfigAnnotationAsset()
                    .setGlobally(Retry.class, "maxRetries", "5")
                    .set(BeanWithRetry.class, "triggerException", Retry.class, "maxRetries", "6")
                    .set(BeanWithRetry.class, "triggerException", Retry.class, "maxRetries", "7")
                    .set(BeanWithRetry.class, "doProcessWithCustomException", Retry.class, "retryOn",
                        CustomRetryServiceException.class.getName()),
                "microprofile-config.properties")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
            .create(WebArchive.class, "ftConfigTest.war")
            .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test that config property on method takes precedence on class and global config properties
     */
    @Test
    void propertyPriorityTest() {
        try {
            bean.triggerException();
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }
        Assert.assertEquals(bean.getRetry(), 8);
    }

    /**
     * Test that config property on file has precedence over annotation config
     */
    @Test
    void propertyRetryOnPriorityTest() {
        try {
            bean.doProcessWithCustomException();
        }
        catch (CustomRetryServiceException e) {
            Assert.assertTrue(e instanceof Exception);
        }
        Assert.assertEquals(bean.getRetry(), 6);
    }

    @Inject
    private BeanWithRetry bean;

}
