## @delegate annotation

Generate proxies for arbitrary traits / classes.

To use simply add `"dev.zio" %% "zio-macros-core" % "<version>"` to your `libraryDependencies`.

This annotation can only be used on a  constructur parameter in a class definition.
This will do a number of things to the resulting class definitions:

* The class will additionally extend any traits extended by the annotated member.
```scala
import zio.macros.annotation.delegate

trait Foo {
  def foo: Int = 4
}
object FooImpl extends Foo

class Bar(@delegate f: Foo)
val b: Foo = new Bar(FooImpl)
```

* Any methods on the resulting type of the defined class that are also defined on the annotated member will be forwarded to the member unless a definition exists in the body of the class.
```scala
import zio.delegate._

trait Foo {
  def foo: Int
}
abstract class Foo1 extends Foo {
  def foo = 4
  def foo1: Int
}

class Bar(@delegate f: Foo)
println(new Bar(new Foo {}).foo) // 4

class Bar1(@delegate f: Foo) {
  def foo = 3
}
println(new Bar1(new Foo {}).foo) // 3

// classes have to be explicitly extended. Forwarders will still
// be automatically generated though.
class Bar2(@delegate f: Foo1) exends Foo1
println(new Bar1(new Foo1 { def foo1 = 3 }).foo1) // 3
```

* The behavior of the annotation can be customized with three options
  ```scala
  class delegate(verbose: Boolean = false, forwardObjectMethods: Boolean = false, generateTraits: Boolean = true)
  ```
  - verbose: The generated class will be reported during compilation. This is very useful for debugging behavior of the annotation or getting a feel for the generated code.
  - forwardObjectMethods: controls whether methods defined on Object and similiar classes should be forwarded. The list of methods affected by this is currently:
  ```scala
  Set(
    "java.lang.Object.clone",
    "java.lang.Object.hashCode",
    "java.lang.Object.finalize",
    "java.lang.Object.equals",
    "java.lang.Object.toString",
    "scala.Any.getClass"
  )
  ```
  - generateTraits: Whether the class definition should be modified to automatically extend any traits defined on the annotated member. If set to false only methods of traits / classes that are explicitly extended will be forwarded.
