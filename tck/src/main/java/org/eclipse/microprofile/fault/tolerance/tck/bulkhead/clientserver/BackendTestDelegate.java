package org.eclipse.microprofile.fault.tolerance.tck.bulkhead.clientserver;

import java.util.concurrent.Future;

/**
 * Common interface for backend test delegates
 * (not all methods need return Futures)
 */
public interface BackendTestDelegate {
    /**
     * Called once per test from inside the
     * Bulkhead
     * @return the 'Future' result
     */
    public Future perform();
}
