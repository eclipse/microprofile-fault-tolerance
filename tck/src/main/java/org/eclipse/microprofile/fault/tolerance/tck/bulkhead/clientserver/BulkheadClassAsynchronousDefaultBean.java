package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple class level Asynchronous @Bulkhead
 *
 * @author Gordon Hutchison
 */
@Bulkhead
@Asynchronous
public class BulkheadClassAsynchronousDefaultBean implements BulkheadTestBackend {

    @Override
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName());
        return action.perform();
    }

};