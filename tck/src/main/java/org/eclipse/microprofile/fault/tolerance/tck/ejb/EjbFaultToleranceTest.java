/*
 *******************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.ejb;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Test for mixed interceptors CDI, EJB and, FT
 */
public class EjbFaultToleranceTest extends Arquillian {

    @Deployment(testable = false)
    public static WebArchive deploy() {
        JavaArchive testJar = ShrinkWrap
            .create(JavaArchive.class, "EjbFtCdi.jar")
            .addClasses(EjbComponent.class, EarlyFtInterceptor.class, LateFtInterceptor.class, CounterFactory.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .as(JavaArchive.class);

        WebArchive war = ShrinkWrap.create(WebArchive.class, "EjbFtCdi.war")
            .addClass(EjbFaultToleranceServlet.class)
            .addAsLibrary(testJar);
        return war;
    }

    @ArquillianResource
    private URL base;

    @Test
    @RunAsClient
    public void servletTest() throws MalformedURLException {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(URI.create(new URL(base, "/EjbFaultToleranceServletTest?name=hello").toExternalForm()));

        Response response = target.request().get();
        Assert.assertEquals(response.readEntity(String.class), "hello");
    }


}
