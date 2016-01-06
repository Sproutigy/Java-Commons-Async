# Sproutigy Java Commons Async
# Async Programming Toolkit for Java

Helps to code asynchronous operations and introduces Promises/Deferred implementation with extended support for reactive collections. Designed to work with Java 8 lambdas.
Thanks to Reactive Streams based implementation and Java's `Future<T>` support, this library may be easily used to cooperate with other reactive libraries.  

Inspired by [Promises A+ specification](https://github.com/promises-aplus/promises-spec) and [RxJava library](https://github.com/ReactiveX/RxJava).

## Requirements and dependencies
Requires Java 8.
This library has following dependencies: 
- [Reactive Streams](http://www.reactive-streams.org/) - required
- [SLF4J](http://www.slf4j.org/) - required
- [Google Guava](https://github.com/google/guava) - optional - to adapt from popular ListenableFuture
- Netty (http://netty.io/) - optional - to adapt from Netty's Future


## Functionality

### Promises

#### Promise
`Promise<T>` interface is an extension of `Future<T>` that allows to listen events or chain further processing.

#### PromiseCollect
`PromiseCollect<E>` represents non-single valued `Promise` that collects multiple elements and finally results with a `Collection<E>`.

#### Awaiting for result
Thread may want to wait for completion and check the result.  
```java
promise.await(); //may be called explicitly, but isSuccess()/isFailure()/getValue()/getCause() would do the same automatically 
if (promise.isSuccess()) {
    System.out.println(promise.getValue());
} else if (promise.isFailure()) {
    promise.getCause().printStackTrace();
}
```

It can be done also in a classic `Future<T>` way:
```java
try {
    Object value = promise.get()
} catch(ExecutionException e) {
    e.getCause().printStackTrace();    
} catch(InterruptedException e) {
    e.printStackTrace();
}
```

Or using `sync()` method:
```java
try {
    Object value = promise.sync()
} catch(Throwable e) {
    e.printStackTrace();    
}
```

Awaiting with declared timeout:
```java
try {
    promise.await(10, TimeUnit.SECONDS);
    System.out.println("Result: " + promise.getValue());
} catch(TimeoutException timeout) {
    System.out.println("I will not wait any longer");
} catch(Exception e) {
    e.printStackTrace()
}
```


#### Listening (callbacks)
Asynchronous operation may be listened for progress and completion. Promise may have multiple listeners attached.
Examples:
```java
promise.onSuccessBlocking( value -> System.out.println("Computed value: " + value) );
promise.onSuccessAsync( value -> {
    //do some long-running operation, e.g. send by e-mail
});

promise.onFailureBlocking( error -> error.printStackTrace() );

promise.addProgressListener( progress -> System.out.println("Progress: " + progress) );

promiseCollect.addElementListener( element -> System.out.println("New element received: " + element) );
promiseCollect.onSuccessBlocking( collection -> System.out.println("Total: " + collection.count() ) ); 
```

It is guaranteed that listener will be called even when processing is finished.
For Collection-based Promises, all elements will be notified, even when Promise is already complete.
Only progress listener is notified right after progress update (so Promise does not keep progress history).
So it is safe to use listeners without the scare of race conditions.

#### Promise States
`PromiseState` enumeration declares 5 states:
- *Waiting* - queued or waiting for external conditions
- *Pending* - operation in progress
- *Succeeded* - done without error
- *Failed* - exception has been thrown during processing
- *Cancelled* - operation has been cancelled

#### PromiseFactory
`PromiseFactory` class defines ways of Promise building. `PromiseFactory.DEFAULT` provides a factory based on `Async.commonExecutor()` for asynchronous executions. 
`PromiseFactory` may be constructed with specific `Executor` for asynchronous calls, as also it's methods could be overridden.
`PromiseFactoryDecorator` has been 
Example of a factory that provides queued promises:
```java
PromiseFactory factory = new DefaultPromiseFactory(Executors.newSingleThreadExecutor());
Promise p1 = factory.async( () -> { Sleep.untilInterrupted(5, TimeUnit.SECONDS); System.out.println("FIRST"); } );
Promise p2 = factory.async( () -> { Sleep.untilInterrupted(5, TimeUnit.SECONDS); System.out.println("SECOND"); } );
```

#### Instant promises
`PromiseFactory` may provide promises that does not require waiting for result by calling `instant`-prefixed methods. Examples:
Examples:
```java
PromiseFactory factory = PromiseFactory.DEFAULT;
Promise<String> p1 = factory.instant("OK");
Promise<String> p2 = factory.instantFailure(new Exception("Sorry Winnetou"));
PromiseCollect<Integer> p3 = factory.instantCollect(new Integer[]{5, 10, 15});
PromiseCollect<Integer> p4 = factory.instantCollectEmpty();
```

### Run operation asynchronously
`PromiseFactory` may create promise based on `Runnable`, `Callable<T>` and `Processor<IN, OUT>`.

Examples:
```java
Promise<Void> p1 = PromiseFactory.DEFAULT.async( () -> System.out.println("ASYNC") );
Promise<Integer> p2 = PromiseFactory.DEFAULT.async( () -> { int x=10; x*=10; return x; });
Promise<Integer> p3 = PromiseFactory.DEFAULT.async( (in) -> in*in, 10 );
```

#### Deferred promises
`Deferred` and `DeferredCollect` are interfaces that controls associated promise state that can be get using `promise()` method.

Example of single valued promise:
```java
public Promise<Integer> theAnswerToLifeUniverseAndEverything() {
    Deferred<Integer> deferred = PromiseFactory.DEFAULT.defer();

    //could be called in asynchronous handlers:
    deferred.pending();
    deferred.progress(0);
    deferred.progress(0.5);
    deferred.progress(1.0);
    deferred.success(42);

    return deferred.promise();
}
```

Example of multi-valued promise:
```java
public PromiseCollect<Integer> lottery(final int total) {
    DeferredCollect<Integer> deferred = PromiseFactory.DEFAULT.deferCollect();

    Async.newThread(() -> {
        deferred.pending();

        int left = total;
        Random random = new Random();
        while (left > 0) {
            Sleep.untilInterrupted(1, TimeUnit.SECONDS);
            deferred.next(random.nextInt(49)+1);
            left--;
        }

        deferred.complete();
    }).start();

    return deferred.promise();
}
```
As `PromiseCollect<E>` implements `Iterable<E>` which results in `ReactiveIterator<E>`, above code may be called as simple as:
```java
for(Integer number : lottery(6)) {
    System.out.println(number);
}
```
As a result, every second new element will arrive to the iterator and will be printed out.

#### Promise from external async operation
`PromiseFactory` provides `from` methods that allows to build `Promise` or `PromiseCollect` from object that represents asynchronous operation.

Examples:

```java
Future<Integer> myFuture = ...;
Promise<Integer> myPromise = PromiseFactory.DEFAULT.from(myFuture);
```

```java
Publisher<Integer> myPublisher = ...;
PromiseCollect<Integer> myPromiseCollect = PromiseFactory.DEFAULT.from(myPublisher);
```

#### Process chain
Operations based on promises may be chained using fluent API, example: 
```java
PromiseFactory.DEFAULT
    .async(() -> "Hello")
    .thenBlocking(s -> s + " world")  //will be computed in the same thread right after completion
    .then(in -> {  //may be computed in other thread 
        System.out.println(in);
        return in;
    })
    .then(in -> {  //guaranteed that it will be started AFTER previous `then` operation will be done
        throw new RuntimeException("Just some error");
    })
    .thenBlocking(in -> {
        System.out.println("Never called because of an exception");
        return "whatever, ignored";
    })
    .catchFail(e -> "No problem")  //`catchFail` may translate error into a value or other exception
    .done();  //checks for non-handled errors
```

#### Joining
Promises may be joined using `all()` and `any()` static methods of `Promises` helper.

Wait for all - example:
```java
Promise pAll = Promises.all(p1, p2, p3);
pAll.await();
```

Wait for any - example:
```java
Promise pAny = Promises.any(p1, p2, p3);
pAny.await();
```

#### Global watch
Promises could be globally watched by decorating `PromiseFactory` or use `addCreationListener()` method.
Example:
```java
PromiseFactory.DEFAULT.addCreationListener(promise -> {
    System.out.println("PROMISE CREATED: " + promise.getIdentifier());
    long startTime = System.currentTimeMillis();
    promise.addStateListener( (myPromise, state) -> {
        System.out.println(myPromise.getIdentifier() + " " + state.toString() + " after " + (System.currentTimeMillis() - startTime) + " millis");
    });
});
```


### Multi-threading

#### ExecutorServiceFactory
Allows to specify and build `ExecutorService`. By default it creates `ExecutorService` with daemon threads and priority less than NORMAL but higher than MINIMAL, with maximum thread pool size 100*(processor cores) having keep-alive time of a unused thread set to 30 seconds.
By default on JVM shutdown for each created `ExecutorService` there will be shutdown attempt.  

Build settings can be modified, example:
```java
ExecutorServiceFactory factory = new ExecutorServiceFactory();
factory.setKeepAliveTime(10, TimeUnit.SECONDS);
factory.setMaximumPoolSize(3);
factory.setCorePoolSize(1);
factory.setThreadPriority(Thread.NORM_PRIORITY + 1);
factory.setRegisterShutdownHook(false);

ExecutorService myExecutorService = factory.newExecutorService();
myExecutorService.execute( () -> System.out.println("Hello world from " + Thread.currentThread().toString()) );
```

`ExecutorServiceFactory` implements `ThreadFactory`, so it may produce new threads when needed (`newThread(Runnable r)`).

#### Common asynchronous thread pool (executor)
`Async.commonExecutor()` is an instance of `ScheduledExecutorService` that could be shared among application. 

#### Thread creation
`Async.newThread(Runnable r)` creates daemon thread with priority between NORMAL and MINIMAL. 


### Reactive Streams

#### BlockingIterator
`BlockingIterator<T>` allows to synchronously iterate over Reactive Stream's `Publisher<T>`, while continuously requesting and receiving data with the ability of cancellation (thanks to `CloseableIterator<T>` interface).
Supports both finite and infinite streams.

Example:
```java
BlockingIterator<Integer> iterator = new BlockingIterator<>(publisher);

List<Integer> received = new LinkedList<>();
while(iterator.hasNext()) {
    Integer element = iterator.next();
    System.out.println(element);
    received.add(element);
    if(received.size() == 10) {
        iterator.close()
        break;   
    }
}
```

If cancellation of a stream is not required and you want to use for-loop, `BlockingIterable` may be used for simplicity:
```java
for(Integer element : new BlockingIterable<Integer>(publisher)) {
    System.out.println(element);
}
```


### Helpers

#### Close
To close ```Closeable``` and ```AutoClosable``` instances, helpful one-line `Close` utility can be used (depending on needs):
```java
Close.unchecked(stream); //may throw unchecked exception 
Close.silently(stream); //catches and ignores exception
Close.loggable(stream); //catches and logs exception
Close.loggableAndThrowable(stream); //catches, logs exception and rethrows it
```

#### Sleep
To delay current thread's processing, one-line `Sleep` utility may be used (depending on needs):
```java
Sleep.interruptable(10, TimeUnit.SECONDS); //may throw InterruptedException
Sleep.unchecked(10, TimeUnit.SECONDS); //may throw unchecked exception
Sleep.untilInterrupted(10, TimeUnit.SECONDS); //never throws exception
```


## Maven

To use as a dependency add to your `pom.xml` into `<dependencies>` section: 
```xml
<dependency>
    <groupId>com.sproutigy.commons</groupId>
    <artifactId>async</artifactId>
    <version>RELEASE</version>
</dependency>
```


## More
For more information and commercial support visit [Sproutigy](http://www.sproutigy.com/opensource)
