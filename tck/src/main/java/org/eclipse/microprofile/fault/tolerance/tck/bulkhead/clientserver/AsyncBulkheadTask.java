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
package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

/**
 * Manages the execution of an asynchronous call to
 * {@link BulkheadTestBackend#test(BackendTestDelegate)}
 * <p>
 * The {@link #perform()} method will not return until {@link #complete()} has
 * been called.
 * <p>
 * {@link #assertStarting()} and {@link #assertNotStarting()} can be used to
 * test whether the method starts executing within a short period.
 * <p>
 * Example usage:
 * <pre><code>AsyncBulkheadTask task = new AsyncBulkheadTask();
 * Future result = asyncBeanUnderTest.call(task);
 * task.assertStarting(result);
 * assertFalse(result.isDone());
 * task.complete("Foo");
 * assertEquals(result.get(2, SECONDS), "Foo");
 * </code></pre>
 */
public class AsyncBulkheadTask extends AbstractBulkheadTask implements BackendTestDelegate {

    @Override
    public Future perform() throws InterruptedException {
        return new TestDelegate().perform();
    }

}
