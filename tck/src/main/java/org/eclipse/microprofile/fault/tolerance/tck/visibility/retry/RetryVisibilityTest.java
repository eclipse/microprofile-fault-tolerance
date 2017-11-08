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
package org.eclipse.microprofile.fault.tolerance.tck.visibility.retry;

import java.io.IOException;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for annotations discovering rules in the contexts of class inheritance, method override, etc.
 * 
 *  @author <a href="mailto:matthieu@brouillard.fr">Matthieu Brouillard</a>
 */
public class RetryVisibilityTest extends Arquillian {
    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftRetryVisibility.jar")
                .addClasses(
                        RS.class, 
                        RetryServiceType.class,
                        RetryService.class, 
                        BaseRetryService.class, 
                        RetryServiceOverrideClassLevel.class, 
                        RetryServiceOverrideMethodLevel.class, 
                        RetryServiceMethodSuppressLevel.class
                )
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetryVisibility.war")
                .addAsLibrary(testJar);
        
        return war;
    }
    
    @Inject @RS(RetryServiceType.BASE)
    private BaseRetryService baseService;
    
    @Inject @RS(RetryServiceType.CLASS_OVERRIDE)
    private RetryServiceOverrideClassLevel serviceOverrideClassLevel;
    
    @Inject @RS(RetryServiceType.METHOD_OVERRIDE)
    private RetryServiceOverrideMethodLevel serviceOverrideMethodLevel;
    
    @Inject @RS(RetryServiceType.METHOD_SUPPRESS)
    private RetryServiceMethodSuppressLevel serviceSuppressMethodLevel;
    
    @Test
    public void baseRetryServiceUsesDefaults() {
        int nbExpectedRetries = 3; // see BaseRetryService class annotation

        checkServiceCall(nbExpectedRetries, this.baseService, "baseRetryServiceUsesDefaults");
    }

    @Test
    public void serviceOverrideClassLevelUsesClassLevelAnnotation() {
        int nbExpectedRetries = 4;  // see RetryServiceOverrideClassLevel class annotation

        checkServiceCall(nbExpectedRetries, this.baseService, "serviceOverrideClassLevelUsesClassLevelAnnotation");
    }

    @Test
    public void serviceOverrideMethodLevelUsesMethodLevelAnnotation() {
        int nbExpectedRetries = 4;   // see RetryServiceOverrideMethodLevel#service() method annotation

        checkServiceCall(nbExpectedRetries, this.baseService, "serviceOverrideMethodLevelUsesMethodLevelAnnotation");
    }
    
    @Test
    public void serviceRetryRemovedAtMethodLevel() {
        int nbExpectedRetries = 0;  // see RetryServiceMethodSuppressLevel#service() method with no annotation

        checkServiceCall(nbExpectedRetries, this.baseService, "serviceRetryRemovedAtMethodLevel");
    }

    private void checkServiceCall(int nbExpectedRetries, RetryService service, String testName) {
        int expectedNbCalls = nbExpectedRetries + 1;
        try {
            service.service();
            Assert.fail(String.format("in %s#%s service() should have failed", 
                    RetryVisibilityTest.class.getSimpleName(), testName)
            );
        } 
        catch (IOException re) {
            Assert.assertEquals(
                service.getNumberOfServiceCalls(),
                    expectedNbCalls, 
                String.format("in %s#%s service() should have been called exactly %d times",
                    RetryVisibilityTest.class.getSimpleName(),
                        testName,
                        expectedNbCalls)
            );
        } 
        catch (RuntimeException ex) {
            Assert.fail(String.format("no %s exception should have been thrown in %s#%s", 
                    ex.getClass().getName(), 
                    RetryVisibilityTest.class.getSimpleName(),
                    testName)
            );
        }
    }
}
