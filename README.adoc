//
// Copyright (c) 2016-2019 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// You may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
image:https://badges.gitter.im/eclipse/microprofile-fault-tolerance.svg[link="https://gitter.im/eclipse/microprofile-fault-tolerance"]

# Eclipse MicroProfile Fault Tolerance

## Introduction

It is increasingly important to build fault tolerant micro services. Fault tolerance is about leveraging different strategies to guide the execution and result of some logic. Retry policies, bulkheads, and circuit breakers are popular concepts in this area. They dictate whether and when executions should take place, and fallbacks offer an alternative result when an execution does not complete successfully.

## Overview 

Fault Tolerance provides developers with the following strategies for dealing with failure:

* Timeout: Define a maximum duration for execution
* Retry: Attempt execution again if it fails
* Bulkhead: Limit concurrent execution so that failures in that area can't overload the whole system
* CircuitBreaker: Automatically fail fast when execution repeatedly fails
* Fallback: Provide an alternative solution when execution fails

Fault Tolerance provides an annotation for each strategy which can be placed on the methods of CDI beans. When an annotated method is called, the call is intercepted and the corresponding fault tolerance strategies are applied to the execution of that method.

## Documentation

For links to the latest maven artifacts, Javadoc and specification document, see the link:https://github.com/eclipse/microprofile-fault-tolerance/releases/latest[latest release].

## Example

Apply the retry and fallback strategies to `doWork()`. It will be executed up to two additional times if if throws an exception. If all executions throw an exception, `doWorkFallback()` will be called and the result of that returned instead.

```java
@ApplicationScoped
public class FaultToleranceBean {
   
   @Retry(maxRetries = 2)
   @Fallback(fallbackMethod = "doWorkFallback")
   public Result doWork() {
      return callServiceA(); // This service usually works but sometimes
                             // throws a RuntimeException
   }
   
   private Result doWorkFallback() {
      return Result.emptyResult();
   }
}
```

From elsewhere, inject the `FaultToleranceBean` and call the method:

```java
@ApplicationScoped
public class TestBean {

    @Inject private FaultToleranceBean faultToleranceBean;
    
    public void test() {
        Result theResult = faultToleranceBean.doWork();
    }
}
```

### Configuration

The annotation parameters can be configured via MicroProfile Config. For example, imagine you have the following code in your application:

```java
package org.microprofile.readme;

@ApplicationScoped
public class FaultToleranceBean {

   @Retry(maxRetries = 2)
   public Result doWork() {
      return callServiceA(); // This service usually works but sometimes
                             // throws a RuntimeException
   }
}
```

At runtime, you can configure `maxRetries` to be `6` instead of `2` for this method by defining the config property `org.microprofile.readme.FaultToleranceBean/doWork/Retry/maxRetries=6`.

Alternatively, you can configure `maxRetries` to be `6` for all instances of `Retry` in your application by specifying the property `Retry/maxRetries=6`.


## Contributing

Do you want to contribute to this project? link:CONTRIBUTING.adoc[Find out how you can help here].