/*
 *******************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.metrics;

import org.eclipse.microprofile.fault.tolerance.tck.util.TestException;
import org.eclipse.microprofile.faulttolerance.Fallback;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FallbackMetricBean {

    public enum Action {
        PASS, FAIL, NON_FALLBACK_EXCEPTION
    }

    private Action fallbackAction = Action.PASS;

    @Fallback(fallbackMethod = "doFallback", skipOn = NonFallbackException.class)
    public void doWork(Action action) {
        if (action == Action.PASS) {
            return;
        } else if (action == Action.FAIL) {
            throw new TestException();
        } else {
            throw new NonFallbackException();
        }
    }

    public void doFallback(Action action) {
        if (fallbackAction == Action.PASS) {
            return;
        } else if (fallbackAction == Action.FAIL) {
            throw new TestException();
        } else {
            throw new NonFallbackException();
        }
    }

    @Fallback(value = FallbackMetricHandler.class, applyOn = TestException.class)
    public Void doWorkWithHandler(Action action) {
        if (action == Action.PASS) {
            return null;
        } else if (action == Action.FAIL) {
            throw new TestException();
        } else {
            throw new NonFallbackException();
        }
    }

    /**
     * Set whether the fallback method and handler should pass or throw an exception
     *
     * @param action
     *            set to {@link Action} PASS or FAIL
     */
    public void setFallbackAction(Action action) {
        this.fallbackAction = action;
    }

    /**
     * Get whether the fallback method and handler should pass or throw an exception
     *
     * @return {@link Action} PASS or FAIL
     */
    public Action getFallbackAction() {
        return fallbackAction;
    }

    @SuppressWarnings("serial")
    public static class NonFallbackException extends RuntimeException {

        public NonFallbackException() {
            super("Non-fallback exception");
        }

    }
}
