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

import org.eclipse.microprofile.fault.tolerance.tck.retrytimeout.clientserver.RetryTimeoutClient;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testng.Assert;
/**
 * Test the combination of the @Retry and @Timeout annotations.
 *
 * @author <a href="mailto:emijiang@uk.ibm.com">Emily Jiang</a>
 *
 */
@RunWith(Arquillian.class)
public class RetryTimeoutTest {

    private @Inject RetryTimeoutClient clientForRetryTimeout;

    @Deployment
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
                .create(JavaArchive.class, "ftRetryTimeout.jar")
                .addClasses(RetryTimeoutClient.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .as(JavaArchive.class);

        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "ftRetryTimeout.war")
                .addAsLibrary(testJar);
        return TckAdditions.decorate(war);
    }

    /**
     * Test that a Service is retried the expected number of times.
     *
     * A timeout is configured for serviceA and in this case the service should generate Timeout exceptions.
     *
     * The service should be retried.
     */
    @Test
    public void testRetryTimeout() {
        try {
            String result = clientForRetryTimeout.serviceA(1000);
        }
        catch (TimeoutException ex) {
            // Expected
        }
        catch (RuntimeException ex) {
            // Not Expected
            Assert.fail("serviceA should not throw a RuntimeException in testRetryTimeout");
        }

        Assert.assertEquals(clientForRetryTimeout.getCounterForInvokingServiceA(), 2, "The execution count should be 2 (1 retry + 1)");
    }

    /**
     * Test that a Service is retried the expected number of times.
     *
     * A timeout is configured for serviceA but the service should fail before the timeout is reached
     * and generate a RuntimeException.
     *
     * The service should be retried.
     */
    @Test
    public void testRetryNoTimeout() {
        try {
            String result = clientForRetryTimeout.serviceA(10);
        }
        catch (TimeoutException ex) {
            // Not Expected
            Assert.fail("serviceA should not throw a TimeoutException in testRetrytNoTimeout");
        }
        catch (RuntimeException ex) {
            // Expected
        }

        Assert.assertEquals(clientForRetryTimeout.getCounterForInvokingServiceA(), 2, "The execution count should be 2 (1 retry + 1)");
    }
}
