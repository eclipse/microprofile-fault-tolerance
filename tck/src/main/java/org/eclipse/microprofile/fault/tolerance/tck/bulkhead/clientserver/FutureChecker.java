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
 * by the user.
 * 
 * @author Gordon Hutchison
 */
public class FutureChecker extends Checker {

    public FutureChecker(int sleepMillis, TestData td) {
        super(sleepMillis, td);
    }

    public FutureChecker(int sleepMillis) {
        super(sleepMillis, new TestData());
    }

    /*
     * This method is the one called from the business method of the injected
     * object that has the annotations.
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

    public final class TestFuture implements Future<String> {

        private AtomicBoolean done = new AtomicBoolean(false);
        private String result = "";
        private boolean isCancelCalled;
        private boolean isDoneCalled;
        private boolean isIsCancelledCalled;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            isCancelCalled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            isIsCancelledCalled = true;
            return isCancelCalled;
        }

        @Override
        public boolean isDone() {
            isDoneCalled = true;
            return true;
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
