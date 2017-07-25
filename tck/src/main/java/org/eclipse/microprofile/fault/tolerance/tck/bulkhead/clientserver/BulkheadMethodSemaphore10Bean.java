package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple method level Semaphore @Bulkhead(10)
 * 
 * @author Gordon Hutchison
 */

public class BulkheadMethodSemaphore10Bean implements BulkheadTestBackend {

    @Override
    @Bulkhead(10)
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};