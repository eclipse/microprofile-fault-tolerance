/**********************************************************************
* Copyright (c) 2017 Contributors to the Eclipse Foundation 
*
* See the NOTICES file(s) distributed with this work for additional
* information regarding copyright ownership.
*
* All rights reserved. This program and the accompanying materials 
* are made available under the terms of the Apache License, Version 2.0
* which accompanies this distribution and is available at
* http://www.opensource.org/licenses/apache2.0.php
*
* SPDX-License-Identifier: Apache-2.0
**********************************************************************/
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;

/**
 * @author Gordon Hutchison 
 *
 */
public class ParrallelBulkheadTest implements Callable<Future> {

    protected BulkheadTestBackend target;
    protected BackendTestDelegate action;

    /**
     * This class is Callable in parallel and then makes calls to Bulkheaded
     * classes
     * 
     * @param target
     *            the backend bulkheaded test class
     * @param action
     *            a delegate class to get the backend to do different things
     */
    public ParrallelBulkheadTest(BulkheadTestBackend target, BackendTestDelegate action) {
        this.target = target;
        this.action = action;
    }

    /**
     * This class is Callable in parallel and then makes calls to Bulkheaded
     * test classes. This constructor set a default sleeping backend
     * 
     * @param target
     *            the backend bulkheaded test class
     */
    public ParrallelBulkheadTest(BulkheadTestBackend target) {
        this.target = target;
        this.action = new Checker(1000);
    }

    @Override
    public Future call() throws Exception {
        Utils.log("here");
        Utils.log("action " + action);
        Utils.log("target " + target);
        return target.test(action);
    }
}