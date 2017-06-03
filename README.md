# Sproutigy Java Commons Async
# Async Programming Toolkit for Java

Helps to code asynchronous operations and introduces Promises/Deferred implementation with extended support for reactive collections. Designed to work with Java 8 lambdas.
Thanks to Reactive Streams based implementation and Java's `Future<T>` support, this library may be easily used to cooperate with other reactive libraries.  

Inspired by [ECMAScript 6 Promise](https://developer.mozilla.org/pl/docs/Web/JavaScript/Reference/Global_Objects/Promise) / [Promises A+ specification](https://github.com/promises-aplus/promises-spec).

## Requirements and dependencies
Requires Java 8.
This library has following dependencies: 
- [Reactive Streams](http://www.reactive-streams.org/) - required
- [SLF4J](http://www.slf4j.org/) - optional

Optionally supports also other libraries: 
- [Google Guava](https://github.com/google/guava) - optional - handles ListenableFuture
- [Netty](http://netty.io/) - optional - handles Netty's Future


## Functionality

### Promises

#### Promise
`Promise<T>` interface is an extension of `Future<T>` that allows to listen for events or chain further processing.
It is mostly API compatible with ECMAScript Promise / Promise A+ while adding Java-specific extensions.  

#### Awaiting for result
Thread may want to wait for completion and check the result.  
```java
promise.await(); //may be called explicitly, but isSuccess()/isFailure()/getValue()/getCause() would do the same automatically 
if (promise.isFulfilled()) { //success
    System.out.println(promise.getValue());
} else if (promise.isRejected()) { //failure
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

Or using `getResult()` method:
```java
try {
    Object value = promise.getResult()
} catch(Throwable e) {
    e.printStackTrace();    
}
```

Awaiting with declared timeout:
```java
try {
    promise.await(10, TimeUnit.SECONDS);
    System.out.println("Result: " + promise.getResult());
} catch(TimeoutException timeout) {
    System.out.println("I will not wait any longer");
} catch(Exception e) {
    e.printStackTrace()
}
```


#### Listening (callbacks)
Asynchronous operation may be listened completion. Promise may have multiple listeners attached.
Examples:
```java
promise.onFulfilled( value -> System.out.println("Computed value: " + value) );
promise.onRejected( error -> error.printStackTrace() ); 
```

It is guaranteed that listener will be called even when promise is already fulfilled or rejected.
So it is safe to use listeners without the risk of race conditions.

#### Promise States
`PromiseState` enumeration declares 5 states:
- *Pending* - queued or waiting for external conditions
- *Fulfilled* - done without error, result is provided
- *Rejected* - exception has been thrown during processing

#### Instant promises
`PromiseFactory` may provide promises that does not require waiting for result by calling `instant`-prefixed methods. Examples:
Examples:
```java
Promise<String> p1 = Promise.resolve("OK");
Promise<String> p2 = Promise.reject(new Exception("Sorry Winnetou"));
```

### Run operation asynchronously
`Promise.execute()` method may be used to run asynchronous operations: 

Examples:
```java
Promise<Void> p1 = Promise.execute( () -> System.out.println("ASYNC") );
Promise<Integer> p2 = Promise.execute( () -> { int x=10; x*=10; return x; });
Promise<Integer> p3 = Promise.execute( 10, x -> x*x ); //runs lambda using first argument
```

#### Deferred promises
`Deferred` interface controls associated promise state.
It is used in case of complex asynchronous operations.

Example of single valued promise:
```java
public Promise<Integer> theAnswerToLifeUniverseAndEverything() {
    Deferred<Integer> deferred = Promise.defer();
    
    someService.runAsync(callbackResult -> {
        deferred.resolve(callbackResult.value);        
    });

    return deferred.promise();
}
```

#### Promise wrapping pending async operation
`Promise` provides `from()` method that allows to build `Promise` from object that represents asynchronous operation (`Future` instance).

Examples:

```java
Future<Integer> myFuture = ...;
Promise<Integer> myPromise = Promise.from(myFuture);
```

#### Processing chain
Operations based on promises may be chained using fluent API with `then()` and `thenAwait()` methods: 
```java
Promise
    .execute(() -> "Hello")
    .then(s -> s + " world")  //append text to string
    .then(in -> {  //guaranteed that it will be started AFTER previous `then` operation will be done
        System.out.println(in);
        return in;
    })
    .then(in -> {
        throw new RuntimeException("Just some error");
    })
    .thenAwait(in -> otherPromise)
    .then(in -> {
        System.out.println("Never called because of an exception thrown on previous step");
        return "whatever, ignored";
    })
    .catchHandle(e -> "No problem");  //`catchHandle` may translate error into a value or other exception
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
    <version>2.0</version>
</dependency>
```


## More
For more information and commercial support visit [Sproutigy](http://www.sproutigy.com/opensource)
