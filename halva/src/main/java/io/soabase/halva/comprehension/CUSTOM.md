### Custom For Comprehensions

Halva comes bundled with a For Comprehension implementation for `Iterable` collections. However, you can build 
For Comprehension implementations for other Monadic types as well. In the examples directory, there are
implementations for `CompletableFuture` and `Optional`. 

In Halva, custom For Comprehension implementations are generated by the Halva Annotation Processor. You declare 
a class that implements `MonadicForWrapper` that is annotated with `MonadicFor` and the Halva processor generates the class during normal 
javac compilation (you may need to [enable Java Annotation processing](../../../../../../../../IDEs.md) in your IDE/build tool).

#### MonadicForWrapper

Your class that implements MonadicForWrapper must create implementations for `flatMap()`, `map()`, and optionally
`filter()`. Halva will generated class will be named with the same name as your annotated class plus `For`. E.g if your annotated
class is named "MyFactory" the generated class will be named "MyFactoryFor". However, if you were to 
name the annotated source class ending in `For`,  the generated class would be named differently. E.g. if your annotated
class is named "MyForFactory", the generated class would be named "MyFor". You can change these defaults with the 
MonadicFor attributes `suffix()` and `unsuffix()`.

#### Examples

| Source Class     | Generated Class     |
|------------------|----------------------|
| [FutureForFactory.java](../../../../../../../../examples/example-generated/FutureForFactory.java) | [FutureFor.java](../../../../../../../../examples/example-generated/FutureFor.java) | 
| [OptionalForFactory.java](../../../../../../../../examples/example-generated/OptionalForFactory.java) | [FutureFor.java](../../../../../../../../examples/example-generated/OptionalFor.java) | 