package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple method level Semaphore @Bulkhead(3)
 * 
 * @author Gordon Hutchison
 */
@ApplicationScoped
public class BulkheadMethodSemaphore3Bean implements BulkheadTestBackend {

    @Override
    @Bulkhead(3)
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};