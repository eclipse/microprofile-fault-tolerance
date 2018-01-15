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
//                .addClasses(
//                        RS.class, 
//                        RetryServiceType.class,
//                        RetryService.class, 
//                        BaseRetryOnClassService.class,
//                        RetryOnClassServiceOverrideClassLevel.class,
//                        RetryOnClassServiceOverrideMethodLevel.class,
//                        RetryOnClassServiceNoAnnotationOnOveriddenMethod.class
//                )
                .addPackage("org.eclipse.microprofile.fault.tolerance.tck.visibility.retry")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetryVisibility.war")
                .addAsLibrary(testJar);
        
        return war;
    }
    
    @Inject @RS(RetryServiceType.BASE_ROC)
    private RetryService baseService;
    
    @Inject @RS(RetryServiceType.BASE_ROC_DERIVED_CLASS_NO_REDEFINITION)
    private RetryService serviceDerivedClassNoRedefinition;
    
    @Inject @RS(RetryServiceType.BASE_ROC_RETRY_REDEFINED_ON_CLASS)
    private RetryService serviceOverrideClassLevel;

    @Inject @RS(RetryServiceType.BASE_ROC_RETRY_REDEFINED_ON_CLASS_METHOD_OVERRIDE)
    private RetryService serviceOverrideClassLevelMethodOverride;
    
    @Inject @RS(RetryServiceType.BASE_ROC_RETRY_REDEFINED_ON_METHOD)
    private RetryService serviceOverrideMethodLevel;

    @Inject @RS(RetryServiceType.BASE_ROC_RETRY_MISSING_ON_METHOD)
    private RetryService serviceSuppressMethodLevel;

    @Inject @RS(RetryServiceType.BASE_ROM)
    private RetryService serviceBaseROM;

    @Inject @RS(RetryServiceType.BASE_ROM_RETRY_MISSING_ON_METHOD)
    private RetryService serviceBaseROMRetryMissingOnMethod;

    @Inject @RS(RetryServiceType.BASE_ROM_RETRY_REDEFINED_ON_CLASS)
    private RetryService serviceBaseROMOverridedClassLevelNoMethodOverride;

    @Inject @RS(RetryServiceType.BASE_ROM_RETRY_REDEFINED_ON_CLASS_METHOD_OVERRIDE)
    private RetryService serviceBaseROMOverridedClassLevelMethodOverride;

    @Inject @RS(RetryServiceType.BASE_ROM_RETRY_REDEFINED_ON_METHOD)
    private RetryService serviceBaseROMOverridedMethodLevel;

    @Inject @RS(RetryServiceType.BASE_ROM_DERIVED_CLASS_NO_REDEFINITION)
    private RetryService serviceBaseROMNoRedefinition;

    @Inject @RS(RetryServiceType.BASE_ROCM)
    private RetryService serviceBaseROCM;

    @Inject @RS(RetryServiceType.BASE_ROCM_DERIVED_CLASS_NO_REDEFINITION)
    private RetryService serviceBaseROCMNoRedefinition;

    @Inject @RS(RetryServiceType.BASE_ROCM_RETRY_REDEFINED_ON_CLASS)
    private RetryService serviceBaseROCMOverridedClassLevelNoMethodOverride;

    @Inject @RS(RetryServiceType.BASE_ROCM_RETRY_REDEFINED_ON_CLASS_METHOD_OVERRIDE)
    private RetryService serviceBaseROCMOverridedClassLevelMethodOverride;

    @Inject @RS(RetryServiceType.BASE_ROCM_RETRY_MISSING_ON_METHOD)
    private RetryService serviceBaseROCMRetryMissingOnMethod;
    
    @Test
    public void baseRetryServiceUsesDefaults() {
        int nbExpectedRetries = 3; // see BaseRetryOnClassService class annotation

        checkServiceCall(nbExpectedRetries, this.baseService, "baseRetryServiceUsesDefaults");
    }

    @Test
    public void serviceDerivedClassNoRedefinition() {
        int nbExpectedRetries = 3;  // see RetryOnClassServiceNoRedefinition class

        checkServiceCall(nbExpectedRetries, serviceDerivedClassNoRedefinition, "serviceDerivedClassNoRedefinition");
    }
    
    @Test
    public void serviceOverrideClassLevelUsesClassLevelAnnotation() {
        int nbExpectedRetries = 4;  // see RetryOnClassServiceOverrideClassLevel class annotation

        checkServiceCall(nbExpectedRetries, serviceOverrideClassLevel, "serviceOverrideClassLevelUsesClassLevelAnnotation");
    }

    @Test
    public void serviceOverrideClassLevelUsesClassLevelAnnotationWithMethodOverride() {
        int nbExpectedRetries = 4;  // see RetryOnClassServiceOverrideClassLevelMethodOverride class annotation

        checkServiceCall(
                nbExpectedRetries
                , serviceOverrideClassLevelMethodOverride
                , "serviceOverrideClassLevelUsesClassLevelAnnotationWithMethodOverride"
        );
    }

    @Test
    public void serviceOverrideMethodLevelUsesMethodLevelAnnotation() {
        int nbExpectedRetries = 4;   // see RetryOnClassServiceOverrideMethodLevel#service() method annotation

        checkServiceCall(nbExpectedRetries, serviceOverrideMethodLevel, "serviceOverrideMethodLevelUsesMethodLevelAnnotation");
    }
    
    @Test
    public void serviceRetryRemovedAtMethodLevel() {
        int nbExpectedRetries = 3;  // see RetryOnClassServiceNoAnnotationOnOveriddenMethod#service() method with no annotation

        checkServiceCall(nbExpectedRetries, serviceSuppressMethodLevel, "serviceRetryRemovedAtMethodLevel");
    }

    @Test
    public void serviceBaseROM() {
        int nbExpectedRetries = 3;

        checkServiceCall(nbExpectedRetries, serviceBaseROM, "serviceBaseROM");
    }

    @Test
    public void serviceBaseROMRetryMissingOnMethod() {
        int nbExpectedRetries = 0;

        checkServiceCall(nbExpectedRetries, serviceBaseROMRetryMissingOnMethod, "serviceBaseROMRetryMissingOnMethod");
    }

    @Test
    public void serviceBaseROMOverridedClassLevelNoMethodOverride() {
        int nbExpectedRetries = 4;

        checkServiceCall(nbExpectedRetries, serviceBaseROMOverridedClassLevelNoMethodOverride, "serviceBaseROMOverridedClassLevelNoMethodOverride");
    }

    @Test
    public void serviceBaseROMOverridedClassLevelMethodOverride() {
        int nbExpectedRetries = 3;

        checkServiceCall(nbExpectedRetries, serviceBaseROMOverridedClassLevelMethodOverride, "serviceBaseROMOverridedClassLevelMethodOverride");
    }

    @Test
    public void serviceBaseROMOverridedMethodLevel() {
        int nbExpectedRetries = 4;

        checkServiceCall(nbExpectedRetries, serviceBaseROMOverridedMethodLevel, "serviceBaseROMOverridedMethodLevel");
    }

    @Test
    public void serviceBaseROMNoRedefinition() {
        int nbExpectedRetries = 3;

        checkServiceCall(nbExpectedRetries, serviceBaseROMNoRedefinition, "serviceBaseROMNoRedefinition");
    }

    @Test
    public void serviceBaseROCM() {
        int nbExpectedRetries = 4;

        checkServiceCall(nbExpectedRetries, serviceBaseROCM, "serviceBaseROCM");
    }

    @Test
    public void serviceBaseROCMNoRedefinition() {
        int nbExpectedRetries = 4;

        checkServiceCall(nbExpectedRetries, serviceBaseROCMNoRedefinition, "serviceBaseROCMNoRedefinition");
    }

    @Test
    public void serviceBaseROCMOverridedClassLevelNoMethodOverride() {
        int nbExpectedRetries = 4;

        checkServiceCall(nbExpectedRetries, serviceBaseROCMOverridedClassLevelNoMethodOverride, "serviceBaseROCMOverridedClassLevelNoMethodOverride");
    }

    @Test
    public void serviceBaseROCMOverridedClassLevelMethodOverride() {
        int nbExpectedRetries = 5;

        checkServiceCall(nbExpectedRetries, serviceBaseROCMOverridedClassLevelMethodOverride, "serviceBaseROCMOverridedClassLevelMethodOverride");
    }

    @Test
    public void serviceBaseROCMRetryMissingOnMethod() {
        int nbExpectedRetries = 3;

        checkServiceCall(nbExpectedRetries, serviceBaseROCMRetryMissingOnMethod, "serviceBaseROCMRetryMissingOnMethod");
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
