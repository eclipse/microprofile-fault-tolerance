package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple method level Asynchronous @Bulkhead(10)
 *
 * @author Gordon Hutchison
 */

public class BulkheadMethodAsynchronous10Bean implements BulkheadTestBackend {

    @Override
    @Bulkhead(10)
    @Asynchronous
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};