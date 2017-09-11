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

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelClient;
import org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelMaxDurationClient;
import org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;
/**
 * Test that Fault Tolerance values configured through annotations can be overridden by configuration properties.
 *  
 * The test assumes that the container supports both the MicroProfile Configuration API and the MicroProfile
 * Fault Tolerance API. Configuration Properties are provided in the manifest of the deployed application.
 * 
 * @author <a href="mailto:neil_young@uk.ibm.com">Neil Young</a>
 *
 */
public class ConfigTest extends Arquillian {

    private @Inject ConfigClient clientForConfig;
    private @Inject ConfigClassLevelClient clientForClassLevelConfig;
    private @Inject ConfigClassLevelMaxDurationClient clientForClassLevelMaxDurationConfig;
    
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "ftConfig.jar")
            .addClasses(ConfigClient.class, ConfigClassLevelClient.class, ConfigClassLevelMaxDurationClient.class)
            .addAsManifestResource(new StringAsset(
                "org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClient/serviceA/Retry/maxRetries=3"+
                "\norg.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelClient/Retry/maxRetries=3"+
                "\norg.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClient/serviceC/Retry/maxDuration=1000"+
                "\norg.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelMaxDurationClient/Retry/maxDuration=1000"),
                    "microprofile-config.properties")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftConfig.war")
                .addAsLibrary(testJar);
        return war;
    }

    /**
     * Test the configuration of maxRetries on a specific method. 
     * 
     * The serviceA is annotated with maxRetries = 5, but a configuration property overrides it with a value of 3,
     * so serviceA should be executed 4 times.
     * 
     * The test assumes that the container has been configured with the property,
     * org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClient/serviceA/Retry/maxRetries=3
     */
    @Test
    public void testConfigMaxRetries() {
        try {
            clientForConfig.serviceA();
            
            Assert.fail("serviceA should throw a RuntimeException in testConfigMaxRetries");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        int count = clientForConfig.getCounterForInvokingConnectionService();
        Assert.assertEquals(count, 4, "The max number of execution should be 4");
    }
    
    /**
     * Test the configuration of maxRetries on a class. 
     * 
     * The class is annotated with maxRetries = 5, but a configuration property overrides it with a value of 3,
     * so serviceA should be executed 4 times.
     * 
     * The test assumes that the container has been configured with the property,
     * org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelClient/Retry/maxRetries=3
     */
    @Test
    public void testClassLevelConfigMaxRetries() {
        try {
            clientForClassLevelConfig.serviceA();
            
            Assert.fail("serviceA should throw a RuntimeException in testClassLevelConfigMaxRetries");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        int count = clientForClassLevelConfig.getCounterForInvokingConnectionService();
        Assert.assertEquals(count, 4, "The max number of execution should be 4");
    }
    
    /**
     * Test the configuration of maxRetries on a class. 
     * 
     * The class is annotated with maxRetries = 5. A configuration property overrides it with a value of 3 but serviceB
     * has its own annotation and should be executed 2 times.
     * 
     * The test assumes that the container has been configured with the property,
     * org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelClient/Retry/maxRetries=3
     */
    @Test
    public void testClassLevelConfigMethodOverrideMaxRetries() {
        try {
            clientForClassLevelConfig.serviceB();
            
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelConfigMethodOverrideMaxRetries");
        }
        catch(RuntimeException ex) {
            // Expected
        }
        int count = clientForClassLevelConfig.getCounterForInvokingConnectionService();
        Assert.assertEquals(count, 2, "The max number of execution should be 2");
    }

    /**
     * Test the configuration of maxDuration on a specific method. 
     * 
     * The serviceA is annotated with maxDuration=3000 but a configuration property overrides it with a value of 1000,
     * so serviceA should be executed less than 11 times.
     * 
     * The test assumes that the container has been configured with the property,
     * org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClient/serviceC/Retry/maxDuration=1000
     */
    @Test
    public void testConfigMaxDuration() {
        try {
            clientForConfig.serviceC();
            Assert.fail("serviceC should throw a RuntimeException in testConfigMaxDuration");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        //The writing service invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        int count = clientForConfig.getRetryCountForWritingService();
        Assert.assertTrue(count< 11, "The max retry counter should be less than 11");
    }

    /**
     * Test the configuration of maxDuration on a class. 
     * 
     * The class is annotated with maxDuration=3000 but a configuration property overrides it with a value of 1000
     * so serviceA should be executed less than 11 times.
     * 
     * The test assumes that the container has been configured with the property,
     * org.eclipse.microprofile.fault.tolerance.tck.config.clientserver.ConfigClassLevelMaxDurationClient/Retry/maxDuration=1000
     */
    @Test
    public void testClassLevelConfigMaxDuration() {
        try {
            clientForClassLevelMaxDurationConfig.serviceA();
            Assert.fail("serviceB should throw a RuntimeException in testClassLevelConfigMaxDuration");
        }
        catch(RuntimeException ex) {
            // Expected
        }

        //The writing service invocation takes 100ms plus a jitter of 0-200ms with the max duration of 1000ms, 
        //the max invocation should be less than 10
        int retryCountforWritingService = clientForClassLevelMaxDurationConfig.getRetryCountForWritingService();        
        Assert.assertTrue(retryCountforWritingService< 11, "The max retry counter should be less than 11");
    }
}
