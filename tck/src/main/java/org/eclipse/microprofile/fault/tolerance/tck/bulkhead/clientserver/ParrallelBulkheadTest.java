package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.eclipse.microprofile.fault.tolerance.tck.bulkhead.BulkheadTest;

/**
 * @author Gordon Hutchison
 *
 */
public class ParrallelBulkheadTest implements Callable<Future> {

    protected BulkheadTestBackend target;
    protected BackendTestDelegate action;

    /**
     * This class is Callable in parallel and then makes calls to Bulkheaded
     * classes
     * 
     * @param target
     *            the backend bulkheaded test class
     * @param action
     *            a delegate class to get the backend to do different things
     */
    public ParrallelBulkheadTest(BulkheadTestBackend target, BackendTestDelegate action) {
        this.target = target;
        this.action = action;
    }

    /**
     * This class is Callable in parallel and then makes calls to Bulkheaded
     * test classes. This constructor set a default sleeping backend
     * 
     * @param target
     *            the backend bulkheaded test class
     */
    public ParrallelBulkheadTest(BulkheadTestBackend target) {
        this.target = target;
        this.action = new Checker(1000);
    }

    @Override
    public Future call() throws Exception {
        BulkheadTest.log("here");
        BulkheadTest.log("action " + action);
        BulkheadTest.log("target " + target);
        return target.test(action);
        
    }
}











