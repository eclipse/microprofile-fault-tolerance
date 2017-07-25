package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple class level Queued @Asynchronous @Bulkhead(10)
 * 
 * @author Gordon Hutchison
 */
@Bulkhead(value = 10, waitingThreadQueue = 10)
@Asynchronous
public class BulkheadClassAsynchronousQueueingBean implements BulkheadTestBackend {

    @Override
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName());
        return action.perform();
    }

};