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


import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.eclipse.microprofile.fault.tolerance.tck.util.AsyncCaller;

/**
 * Allows BulkheadTasks to be created and ensures they all get cleaned up at the end of the test
 */
public class BulkheadTaskManager {
    
    private AsyncCaller executor;
    private List<BulkheadTask> startedTasks = new ArrayList<>();
    
    private synchronized AsyncCaller getExecutor() {
        if (executor == null) {
            // Lookup the AsyncCaller bean instance from CDI
            BeanManager bm = CDI.current().getBeanManager();
            Bean<?> asyncCallerBean = bm.resolve(bm.getBeans(AsyncCaller.class));
            executor = (AsyncCaller) bm.getReference(asyncCallerBean, AsyncCaller.class, bm.createCreationalContext(null));
        }
        return executor;
    }
    
    public BulkheadTask startTask(BulkheadTestBackend backend) {
        BulkheadTask task = new BulkheadTask(getExecutor(), backend);
        task.run();
        startedTasks.add(task);
        return task;
    }
    
    /**
     * Makes the BulkheadTaskManager ready to run a new set of tasks.
     * <p>
     * Release any started tasks and waits for them to complete.
     * <p>
     * This should be called at the end of each test.
     * 
     * @throws InterruptedException if the thread is interrupted
     */
    public void cleanup() throws InterruptedException {
        for (BulkheadTask task : startedTasks) {
            task.complete();
        }
        
        for (BulkheadTask task : startedTasks) {
            try {
                task.awaitFinished(1, TimeUnit.MINUTES);
            }
            catch (TimeoutException e) {
                fail("Unable to clean up all tasks");
            }
        }
        
        startedTasks = new ArrayList<>();
    }

}
