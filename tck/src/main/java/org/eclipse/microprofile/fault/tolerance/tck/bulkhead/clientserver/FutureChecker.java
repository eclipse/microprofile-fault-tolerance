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
 * A simple sleeping test backend worker. Having this backend as a delegate
 * means that we can perform more than one kind of test using a common
 * 
 * @Injected object that delegates to one of these that is passed in as a
 *           parameter to the business method.
 * 
 *           There are a number of tests that this backend can perform:
 *           <ul>
 *           <li>expected number of instances created
 *           <li>expected workers started via perform method
 *           <li>max simultaneous workers not exceeded
 *           </ul>
 * 
 * @author Gordon Hutchison
 */
public class FutureChecker extends Checker {

    public FutureChecker(int sleepMillis) {
        super(sleepMillis);
    }

    public final class TestFuture implements Future<String> {

        private AtomicBoolean cancelCalled = new AtomicBoolean(false);
        private AtomicBoolean done = new AtomicBoolean(false);
        private AtomicBoolean interrupted = new AtomicBoolean(false);
        private String result;
        private int millis;

        public TestFuture(int millis) {
            this.millis = millis;
            try {
                Thread.sleep(millis);
            }
            catch (InterruptedException e) {
             Utils.log(e.toString());
            }
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            Utils.log("Cancel");
            result = result + "CANCELED.";
            cancelCalled.set(true);
            done.set(true);
            return true;
        }

        @Override
        public boolean isCancelled() {
            Utils.log("isCancelled");
            result = result + "IS_CANCELED.";
            return cancelCalled.get();
        }

        @Override
        public boolean isDone() {
            Utils.log("isDone");
            result = result + "IS_DONE.";
            return done.get();
        }

        @Override
        public String get() throws InterruptedException, ExecutionException {
            Utils.log("Get");
            result = result + "GET.";
            work();
            done.set(true);
            return result;
        }

        @Override
        public String get(long timeout, TimeUnit unit) {
            Utils.log("getTO");
            result = result + "GET_TO.";
            work();
            done.set(true);
            return result;
        }

        private void work() {
            try {
                if (!done.get()) {
                //    Thread.sleep(millis);
                }
            }
            catch (Throwable t) {
                Utils.log(t.toString());
                interrupted.set(true);
            } 
            finally {
                done.set(true);
            }
        }

    }

    /*
     * Work this is the method that simulates the backend work inside the
     * Bulkhead.
     *
     * @see org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver.
     * BulkheadTestAction#perform()
     */
    @Override
    public Future<String> perform() {
        return new TestFuture(millis);
    }

}
