package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple class level Semaphore @Bulkhead
 *
 * @author Gordon Hutchison
 */
@Bulkhead
public class BulkheadClassSemaphoreDefaultBean implements BulkheadTestBackend {

    @Override
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};