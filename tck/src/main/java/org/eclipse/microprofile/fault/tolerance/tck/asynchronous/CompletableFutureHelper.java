package org.eclipse.microprofile.fault.tolerance.tck.asynchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 *
 * @author Ondrej Mihalyi
 */
public class CompletableFutureHelper {

    private CompletableFutureHelper() { // this is a util class only for static methods
    }

    /**
     * Creates a future completed with a supplied exception.
     * Equivalent to {@link CompletableFuture}{@code .failedFuture} available since Java 9 but not in Java 8.
     * 
     * @param <U> The type of the future result
     * @param ex The exception to finish the result with
     * @return A future completed with the a supplied exception {@code ex}
     */
    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        CompletableFuture<U> future = new CompletableFuture<>();
        future.completeExceptionally(ex);
        return future;
    }

    /**
     * Returns a future that is completed when the stage is completed and has the same value or exception as the completed stage. It's supposed to be equivalent to calling {@link CompletionStage#toCompletableFuture()} but works with any CompletionStage and doesn't throw {@link java.lang.UnsupportedOperationException}.
     * @param <U> The type of the future result
     * @param stage Stage to convert to a future
     * @return Future converted from stage
     */
    public static <U> CompletableFuture<U> toCompletableFuture(CompletionStage<U> stage) {
        CompletableFuture future = new CompletableFuture();
        stage.whenComplete((v, e) -> {
            if (e != null) {
                future.completeExceptionally(e);
            } else {
                future.complete(v);
            }
        });
        return future;
    }
}