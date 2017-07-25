package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

/**
 * This is a common backend for @Bulkhead tests. As it might be used
 * for @Asynchronous testing all the methods have to return a future. For common
 * code that has methods that don't return a Future delegate to the
 * BulkheadTestAction class
 * 
 * @author Gordon Hutchison
 */
public interface BulkheadTestBackend {

    /**
     * Perform the test
     * 
     * @param action
     * @return a Future compatible with @Asynchronous @Bulkhead tests.
     */
    public Future test(BackendTestDelegate action);

}
