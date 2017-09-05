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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.Utils;

/**
 * A backend that is used in the tests of the operations on the Future returned
 * by the user (only get() should be called).
 * 
 * @author Gordon Hutchison
 */
public class FutureChecker extends Checker {

    public FutureChecker(int sleepMillis, TestData td) {
        super(sleepMillis, td);
    }

    public FutureChecker(int sleepMillis) {
        super( sleepMillis, new TestData());
    }

    /*
     * This method is the one called from the business method of the injected
     * object that has the annotations. In this class we just wait to be told
     * what to do.
     * 
     * @see org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.
     * BulkheadTestAction#perform()
     */
    @Override
    public Future<String> perform() throws InterruptedException {
        try {
            int left = getSleep();
            int tick = 250;
            while (left > 0 && !Thread.currentThread().isInterrupted()) {
                if (left < tick) {
                    Utils.log("tick(" + left + "),");
                    Utils.sleep(left);
                    left = 0;
                }
                else {
                    Utils.log("tick(" + left + "),");
                    Thread.sleep(tick);
                    left = left - tick;
                }
            }
        }
        catch (InterruptedException e) {
            Utils.log("FutureChecker interrupted " + e.toString());
            if (getSleep() > 60 * 1000) {
                throw e;
            }
        }
        return new TestFuture();
    }

    /**
     * This test backend delegate does nothing except to complain with
     * UnsupportedOperation exceptions if methods that are not expected to be
     * called are called...The Future returned from a annotated method or class
     * to a client is not expected to pass on any method calls to the users
     * underlying method result object apart from the get and get with timeout
     * methods. The semantics are that, for example, isDone()'s result are with
     * respect to the operation of running the method, not as the result would
     * be if it was delegated to the Future object that the method returns
     *
     */
    public final class TestFuture implements Future<String> {

        private AtomicBoolean done = new AtomicBoolean(false);
        private String result = "";

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException();
        }

        /*
         * We just return a string representing the methods we have seen called.
         * 
         * @see java.util.concurrent.Future#get()
         */
        @Override
        public String get() throws InterruptedException, ExecutionException {
            result = result + "GET.";
            Utils.log("Result is " + result);
            return result;
        }

        /*
         * We just return a string representing the methods we have seen called.
         * 
         * @see java.util.concurrent.Future#get(long,
         * java.util.concurrent.TimeUnit)
         */
        @Override
        public String get(long timeout, TimeUnit unit) {
            result = result + "GET_TO.";
            Utils.log("Result is " + result);
            return result;
        }

    }

}
