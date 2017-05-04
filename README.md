# Fault Tolerance

* Proposal: [MP-0004](0004-FaultTolerance.md)
* Authors: [Emily Jiang](https://github.com/Emily-Jiang), [Jonathan Halterman](https://github.com/jhalterman/), [Antoine Sabot-Durand](https://github.com/antoinesd), [John Ament](https://github.com/johnament)
* Status: **Awaiting review**

*During the review process, add the following fields as needed:*

* Decision Notes: [Discussion thread topic covering the  Rationale](https://groups.google.com/forum/#!topic/microprofile/ezFC1TLGozU), [Discussion thread topic with additional Commentary](https://groups.google.com/forum/#!forum/microprofile)

## Introduction

It is increasingly important to build fault tolerant micro services. Fault tolerance is about leveraging different strategies to guide the execution and result of some logic. Retry policies, bulkheads, and circuit breakers are popular concepts in this area. They dictate whether and when executions should take place, and fallbacks offer an alternative result when an execution does not complete successfully. 

As mentioned above, the Fault Tolerance proposal is to focus the aspects: TimeOut, RetryPolicy, Fallback, bulkhead and circuit breaker.

* TimeOut: Define a duration for timeout
* RetryPolicy: Define a criteria on when to retry 
* Fallback: provide an alternative solution for a failed execution.
* Bulkhead: isolate failures in part of the system while the rest part of the system can still function.
* CircuitBreaker: offer a way of fail fast by automatically failing execution to prevent the system overloading and indefinite wait or timeout by the clients.

The main design is to separate execution logic from execution. The execution can be configured with fault tolerance policies, such as RetryPolicy, fallback, Bulkhead and CircuitBreaker. 

[Hystrix](https://github.com/Netflix/Hystrix) and [Failsafe](https://github.com/jhalterman/failsafe) are two popular libraries for handling failures. This proposal is to define a standard API and approach for applications to follow in order to achieve the fault tolerance.

The requirements are as follows:

* Loose coupling: Execution logic should not know anything about the execution status or fault tolerance. 
* Failure handling strategy should be configured when the execution takes place.
* Support for synchronous and asynchronous execution
* Integration with 3rd party asynchronous APIs. This is necessary to handle executions that are completed at some time in the future, where retries will need to be explicitly scheduled from within the asynchronous execution. This is common when working with various 3rd party asynchronous tools such as Netty, RxJava, Vert.x, etc.
* Require immutable failure handling policy configuration
* Some Failure policy configurations, e.g. CircuitBreaker, RetryPolicy, can be used stand alone. For example, it has been very useful for circuit breakers to be standalone constructs which can be plugged into and intentionally shared across multiple executions. Likewise for retry policies. Additionally, an Execution construct can be offered that allows retry policies to be applied to some logic in a standalone, manually controlled way.

Advanced requirements:

* Event: Since this approach to fault tolerance involves handing execution over to some foreign code, it's very useful to be able to learn when executions are taking place and under what circumstances (onRetry, onFailedAttempt, onFailure, etc).

Mailinglist thread: [Discussion thread topic for that proposal](https://groups.google.com/forum/#!topic/microprofile/ezFC1TLGozU)

## Motivation

Currently there are at least two libraries to provide fault tolerance. It is best to uniform the technologies and define a standard so that micro service applications can adopt and the implementation of fault tolerance can be provided by the containers if possible.

## Proposed solution

Separate the responsibility of executing logic (Runnables/Callables/etc) from guiding when execution should take place (through retry policies, bulkheads, circuit breakers). In this way, failure handling strategies become configuration that can influence executions, and the execution API itself is just responsible for receiving some configuration and performing executions.

By default, a failure handling strategy could assume, for example, that any exception is a failure. But in some cases, a `null` or negative return value could also be considered a failure. Users should be able to define this, and a user's definition of a failure is what should influence execution. (This all is what the Failsafe RetryPolicy's `retryOn`, `retryWhen`, `abortIf`, etc methods are all about - defining a failure).

Standardise the Fallback, Bulkhead and CircuitBreaker APIs and provide implementations.

* CDI-first approach to apply RetryPolicy, Fallback, BulkHead, CircuitBreaker using annotations

## Detailed design (One example of implemenations)


### CDI-based approach 
Use interceptor and annotation to specify the execution and policy configration.
An annotation of Asynchronous has to be specified for any asynchronous calls. Otherwise, synchronous execution is assumed. 
The implementation should provide two interceptors, one for synchronous invocation and the other for asynchronous invocation. 
```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@Inherited
public @interface Asynchronous {

}
```
#### RetryPolicy: A policy to define the retry criteria

An annotation to specify the max retries, delays, maxDuration, Duration unit, jitter, retryOn, bakeOff, fallback etc.
```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Retry {
   
   /**
     *
     * @return The max number of retries. -1 indicates retry forever.
     * IllegalArgumentException if maxRetries <-1.
     */
    int maxRetries() default 3;

    /**
     * The delay between retries. Defaults to {@link Duration#NONE}.
     * @return
     */
    int delay() default 0;

    /**
     *
     * @return the delay unit
     */

    TimeUnit delayUnit() default TimeUnit.MILLISECONDS;

    /**
     * @return the maximum duration to perform retries for.
     */
    int maxDuration() default 20;

    /**
     *
     * @return the duration unit
     */
    TimeUnit durationUnit() default TimeUnit.MILLISECONDS;

    /**
     *
     * @return the jitter that randomly vary retry delays by. e.g. a jitter of 200 milliseconds
     * will randomly add betweem -200 and 200 milliseconds to each retry delay.
     */
    int jitter() default 200;

    /**
     *
     * @return the jitter delay unit.
     */
    TimeUnit jitterDelayUnit() default TimeUnit.MILLISECONDS;

    /**
     * For each retry delay, a randomly portion of the delay multiplied by the jitterFactor will be added or subtracted to the delay.
     * e.g. a retry delay of 200 milliseconds and a jitter of 0.25 will result in a random retry delay between 150 and 250 milliseconds.
     * @return the jitter factor.
     */

    double jitterFactor() default 0.25;

    /**
     *
     * @return Specify the failure to retry on
     */
    Class<? extends Throwable>[] retryOn() default { Throwable.class };

    /**
     *
     * @return Specify the failure to abort on
     */
    Class<? extends Throwable>[] aboartOn() default { Throwable.class };
	/**
     *
     * @return The fallback method name
     */
    String fallBack();


}


```
#### CircuitBreaker: a rule to define when to close the circuit

```
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface CircuitBreaker {

        /**
     * Define the failure criteria
     * @return the failure exception
     */
    Class<? extends Throwable>[] failOn() default Throwable.class;

    /**
     *
     * @return The delay time after the circuit is open
     */
    long delay() default 2;

    /**
     *
     * @return The delay unit after the circuit is open
     */

    TimeUnit delayUnit() default TimeUnit.SECONDS;

    /**
     *
     * @return The failure threshold to open the circuit
     */
    long failThreshold() default 2;

    /**
     *
     * @return The success threshold to fully close the circuit
     */
    long successThreshold() default 2;

}
```
#### Fallback
```
/**
 * Define the Fallback annotation to specify the fallback callable, BiConsumer or BiFuncation
 *@author Emily Jiang
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Fallback {

    /**
     *
     * @return the fallback class
     */
    Class<?> fallback();

}
```
#### Timeout to be used with either Retry or CircuitBreaker
```
/**
 * The Retry annotation to define the number of the retries and the fallback method on reaching the
 * retry counts.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface TimeOut {

    /**
     *
     * @return the timeout
     */
    long time() default 2;

    /**
     *
     * @return the time unit
     */
    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

}
```
#### Bulkhead - threadpool or semaphore style
```
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Bulkhead {
    public enum Mode {
        THREADPOOL, SEMAPHORE;

        private Mode() {
        }
    }

    Mode value() default Mode.THREADPOOL;
}
```
#### Usage
An interceptor and fault tolerance policy can be applied to a bean or methods.
```
@ApplicationScoped
public class FaultToleranceBean {
   int i = 0;
   @Retry(maxRetries = 2)
   public Runnable doWork() {
      Runnable mainService = () -> serviceA(); // This unreliable service sometimes succeeds but
                                         // sometimes throws a RuntimeException
	  return mainService;								 
   }
}
}
```
### Non-CDI approach
Some application don't use CDI but would like to utilise the fault tolerance feature. 

#### Retry 
```
RetryPolicy rp = FaultToleranceFactory.getInstance(RetryPolicy.class).retryOn(TimeOutException.class)
  .withDelay(2, TimeUnit.SECONDS)
  .withMaxRetries(2);
```

When `TimeOutException` was received, delay 2 seconds and then retry 2 more times.

#### Fallback 
```
Connection connection = FaultToleranceFactory.getInstance(Execution.class).with(rp).withFallBack(this::connectToBackup).get(this::connectToPrimary)
```

If `TimeOutException` is thrown, compute an alternative result such as from a backup resource.

#### CircuitBreaker: a rule to define when to close the circuit

```
CircuitBreaker cb = FaultToleranceFactory.getInstance(CircuitBreaker.class)
  .withFailureThreshold(3, 10)
  .withSuccessThreshold(5)
  .withDelay(1, TimeUnit.MINUTES);
Connection connect = execution.with(cb).run(this::connect);  
```

When 3 of 10 execution failures occurs on a circuit breaker, the circuit is opened and further execution requests fail with `CircuitBreakerOpenException`. After a delay of 1 min, the circuit is half-opened and trail executions are attempted to determine whether the circuit should be closed or opened again. If the trial executions exceed a success threshold 5, the breaker is closed again and executions will proceed as normal.

### Bulkhead

```
BulkHead bh = FaultToleranceFactory.getInstance(Bulkhead.class).withPool("myPool");
Connection connect = FaultToleranceFactory.getInstance(Executor.class).with(bh).run(this::connect);
```

Bulkhead provides a thread pool with a fixed number of threads in order to achieve thread and failure isolation.
### Timeout

```
Connection connect = FaultToleranceFactory.getInstance(Executor.class).withCircuitBreaker(circuitBreaker).withTimeOut(2, TimeUnit.SECONDS).run(this::connect);
```
## Impact on existing code

n/a

## Alternatives considered

n/a
