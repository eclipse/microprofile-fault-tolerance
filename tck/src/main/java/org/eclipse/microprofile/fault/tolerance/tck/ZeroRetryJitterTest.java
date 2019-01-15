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
import static org.hamcrest.Matchers.lessThan;
import static org.testng.Assert.assertEquals;

import javax.inject.Inject;

import org.eclipse.microprofile.fault.tolerance.tck.retry.clientserver.RetryClientForZeroJitter;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;


public class ZeroRetryJitterTest extends Arquillian {

    @Inject
    private RetryClientForZeroJitter zeroJitterClient;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap.create(JavaArchive.class, "ftZeroTestJitter.jar")
                                        .addClasses(RetryClientForZeroJitter.class)
                                        .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                                        .as(JavaArchive.class);

        return ShrinkWrap.create(WebArchive.class, "ftZeroTestJitter.war").addAsLibrary(testJar);
    }

    /**
     * Test that checks that jitter = 0 does not generate error during method call.
     * <p>
     * A Service is annotated with a @Retry annotation with jitter = 0.
     */
    @Test
    public void test() {
        zeroJitterClient.serviceA();
        assertEquals(zeroJitterClient.getRetries(), 3, "Incorrect number of retries");
        assertThat("It took too much time for 3 retries", zeroJitterClient.getTotalRetryTime(), lessThan(3 * 200L));
    }
}
