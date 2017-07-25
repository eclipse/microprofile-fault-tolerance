package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 *  A simple method level Semaphore @Bulkhead(10)
 *
 */

public class BulkheadMethodSemaphoreDefaultBean implements BulkheadTestBackend {

    @Override
    @Bulkhead
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean");
        return action.perform();
    }

};