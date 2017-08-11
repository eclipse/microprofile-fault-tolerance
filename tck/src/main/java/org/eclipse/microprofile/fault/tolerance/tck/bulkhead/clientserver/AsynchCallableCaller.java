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

import org.eclipse.microprofile.faulttolerance.Asynchronous;

/**
 * This class exists as a source of Asynchronicity for other tests.
 * (for example when we want to get more than one thread into a bulkhead)
 * We cannot use a Java EE managed executor (or anything that prereqs
 * a Java EE environment) and for some tests using a vanilla executer service creates
 * problems as threads cannot be branched from 'successfully'. 
 * If we use a @Asynchronous to get a new thread then the framework that is
 * being tested should implement this in a way that will work in either
 * Java EE or Java SE whichever is appropriate for their environment.
 * 
 * @author Gordon Hutchison
 *
 */
@Asynchronous
public class AsynchCallableCaller {

    public Future call(Callable<Future> to) throws Exception {
        return to.call();
    }
}
