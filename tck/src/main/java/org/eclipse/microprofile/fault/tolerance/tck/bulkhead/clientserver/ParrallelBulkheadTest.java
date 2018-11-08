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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.testng.Assert;

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
     * @param td test data
     */
    public ParrallelBulkheadTest(BulkheadTestBackend target, TestData td) {
        this.target = target;
        this.action = new Checker(1000, td);
    }

    @Override
    public Future call() throws Exception {
        Utils.log("action " + action);
        Utils.log("target " + target);
        Future result = null;

        try {
            result = target.test(action);
        }
        catch( BulkheadException b) {
            Utils.log("Might expect a Bulkhead exception from some tests : " + b.toString() + b.getMessage());
        }
        catch( Throwable t ){
            Assert.fail("Unexpected exception: " + t.toString(), t);
        }
        return result;
    }
}