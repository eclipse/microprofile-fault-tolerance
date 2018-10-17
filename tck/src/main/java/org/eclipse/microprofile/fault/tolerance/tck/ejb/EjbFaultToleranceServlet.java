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


import org.eclipse.microprofile.fault.tolerance.tck.ejb.CounterFactory.CounterId;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A Client to demonstrate the ordering behavior of FT annotation, CDI, and EJB interceptors
 *
 */
@WebServlet("/EjbFaultToleranceServletTest")
public class EjbFaultToleranceServlet extends HttpServlet {

    @Inject
    @CounterId("EarlyFtInterceptor")
    private AtomicInteger earlyInterceptorCounter;

    @Inject
    @CounterId("LateFtInterceptor")
    private AtomicInteger lateInterceptorCounter;

    @Inject
    @CounterId("serviceA")
    private AtomicInteger methodCounter;

    @Inject
    @CounterId("connectionService")
    private AtomicInteger methodFailCounter;

    @EJB
    private EjbComponent testEjb;

    public static final String MESSAGE_PARAM = "message";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {

        response.getWriter().append(request.getParameter(MESSAGE_PARAM));

    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException {
        doGet(request, response);
    }
}
