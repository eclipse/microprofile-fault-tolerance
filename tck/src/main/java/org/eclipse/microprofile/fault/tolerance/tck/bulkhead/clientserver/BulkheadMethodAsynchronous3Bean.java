package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple method level Asynchronous @Bulkhead(3)
 *
 * @author Gordon Hutchison
 */
@ApplicationScoped
public class BulkheadMethodAsynchronous3Bean implements BulkheadTestBackend {

    @Override
    @Bulkhead(3)
    @Asynchronous
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};