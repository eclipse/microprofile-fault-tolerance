package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple method level Asynchronous @Bulkhead 
 *
 * @author Gordon Hutchison
 */

public class BulkheadMethodAsynchronousDefaultBean implements BulkheadTestBackend {

    @Override
    @Bulkhead
    @Asynchronous
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};