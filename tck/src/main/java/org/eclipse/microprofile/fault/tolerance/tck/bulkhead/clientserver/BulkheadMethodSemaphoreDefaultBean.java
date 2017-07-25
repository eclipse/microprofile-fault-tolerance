package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple method level Semaphore @Bulkhead
 *
 * @author Gordon Hutchison
 */

public class BulkheadMethodSemaphoreDefaultBean implements BulkheadTestBackend {

    @Override
    @Bulkhead
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};