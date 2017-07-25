package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;

/**
 * A simple class level Asychronous @Bulkhead(3)
 *
 * @author Gordon Hutchison
 */
@Bulkhead(3) @Asynchronous @ApplicationScoped
public class BulkheadClassAsynchronous3Bean implements BulkheadTestBackend {

    @Override
    public Future test(BackendTestDelegate action) {
        BulkheadTest.log("in bean " + this.getClass().getName() );
        return action.perform();
    }

};