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

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Checks standard Java annotation retrievals for Retry annotation.
 *
 *  @author <a href="mailto:matthieu@brouillard.fr">Matthieu Brouillard</a>
 */
public class PureJ2SERetryVisibilityTest {
    @Test
    public void checkRetryVisibilityOnRetryServiceMethodSuppressLevel() throws Exception {
        Retry foundAnnotation;
        Method m = RetryOnClassServiceNoAnnotationOnOveriddenMethod.class.getDeclaredMethod("service");

        foundAnnotation = m.getDeclaredAnnotation(Retry.class);
        Assert.assertNull(foundAnnotation,
                "no Retry annotation should be found on RetryOnClassServiceNoAnnotationOnOveriddenMethod#service() via getDeclaredAnnotation()");

        foundAnnotation = m.getAnnotation(Retry.class);
        Assert.assertNull(foundAnnotation,
                "no Retry annotation should be found on RetryOnClassServiceNoAnnotationOnOveriddenMethod#service() via getAnnotation()");

        foundAnnotation = RetryOnClassServiceNoAnnotationOnOveriddenMethod.class.getDeclaredAnnotation(Retry.class);
        Assert.assertNull(foundAnnotation,
                "no Retry annotation should be found on RetryOnClassServiceNoAnnotationOnOveriddenMethod class via getDeclaredAnnotation()");

        foundAnnotation = RetryOnClassServiceNoAnnotationOnOveriddenMethod.class.getAnnotation(Retry.class);
        Assert.assertNotNull(foundAnnotation,
                "a Retry annotation should have been found because of inheritance on RetryOnClassServiceNoAnnotationOnOveriddenMethod " +
                        "class via getAnnotation()");
    }

    @Test
    public void checkBaseRomRetryMissingOnMethod() throws Exception {
        Retry foundAnnotation;
        Method m = RetryOnMethodServiceNoAnnotationOnOverridedMethod.class.getDeclaredMethod("service");
        
        foundAnnotation = m.getDeclaredAnnotation(Retry.class);
        Assert.assertNull(foundAnnotation,
                "no Retry annotation should be found on RetryOnMethodServiceNoAnnotationOnOverridedMethod#service() " +
                        "via getDeclaredAnnotation()");

        foundAnnotation = m.getAnnotation(Retry.class);
        Assert.assertNull(foundAnnotation,
                "no Retry annotation should be found on RetryOnMethodServiceNoAnnotationOnOverridedMethod#service() via getAnnotation()");
    }
}
