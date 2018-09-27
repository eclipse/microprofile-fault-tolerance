/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.fault.tolerance.tck.util;

import static org.testng.Assert.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

/**
 * Utility class, no public constructor
 */
public class Exceptions {

    private Exceptions() {}
    
    /**
     * Run an action an check that a timeout exception is thrown
     *
     * @param action The action to run
     */
    public static void expectTimeout(ExceptionThrowingAction action) {
        expect(TimeoutException.class, action);
    }

    /**
     * Run an action and check that a {@link TestException} is thrown
     *
     * @param action The action to run
     */
    public static void expectTestException(ExceptionThrowingAction action) {
        expect(TestException.class, action);
    }

    /**
     * Run an action and check that a {@link CircuitBreakerOpenException} is thrown
     *
     * @param action The action to run
     */
    public static void expectCbOpen(ExceptionThrowingAction action) {
        expect(CircuitBreakerOpenException.class, action);
    }
    
    /**
     * Run an action and check that a {@link BulkheadException} is thrown
     *
     * @param action The action to run
     */
    public static void expectBulkheadException(ExceptionThrowingAction action) {
        expect(BulkheadException.class, action);
    }
    
    /**
     * Run {@code future.get()} and check that a {@link BulkheadException} is thrown
     * wrapped in an {@link ExecutionException}.
     *
     * @param future the action to run
     */
    public static void expectBulkheadException(Future<?> future) {
        expect(BulkheadException.class, future);
    }
    
    /**
     * Call {@code future.get()} and check that it throws an ExecutionException wrapping the {@code expectedException}
     * 
     * @param expectedException the expected exception type
     * @param future the future to check
     */
    public static void expect(Class<? extends Exception> expectedException, Future<?> future) {
        try {
            future.get(1, TimeUnit.MINUTES); // Long timeout here to avoid possibility of tests hanging
            fail("Execution exception not thrown from Future");
        }
        catch (ExecutionException e) {
            if (!expectedException.isInstance(e.getCause())) {
                fail("Unexpected exception thrown from Future", e.getCause());
            }
        }
        catch (InterruptedException e) {
            fail("Getting future result was interrupted", e);
        }
        catch (java.util.concurrent.TimeoutException e) {
            fail("Timed out waiting for future to throw " + expectedException.getSimpleName());
        }
    }

    /**
     * Run an action an ensure that the expected exception is thrown
     *
     * @param expectedException the exception class to expect
     * @param action the action to run
     */
    public static void expect(Class<? extends Exception> expectedException, ExceptionThrowingAction action) {
        try {
            action.call();
            fail("Expected exception not thrown. Expected " + expectedException.getName());
        }
        catch (Exception e) {
            if (!expectedException.isInstance(e)) {
                fail("Unexpected exception thrown", e);
            }
        }
    }
    
    @FunctionalInterface
    public static interface ExceptionThrowingAction {
        public void call() throws Exception;
    }


}
