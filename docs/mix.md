## Mix Typeclass

Derive rules to combine instances of traits into a compound type.

To use simply add `"dev.zio" %% "zio-macros-delegate" % "<version>"` to your `libraryDependencies`.

An instance of
```scala
trait Mix[A, B] {

  def mix(a: A, b: B): A with B

}
```
provides evidence that an instance of `B` can be mixed into an instance of `A`.
A macro is defined that can derive an instance of Mix for any two types if the first is a nonfinal class or a trait and the second one is a trait. It can be used like this
```scala
class Foo {
  def foo: Int = 2
}
trait Bar {
  def bar: Int
}
def withBar[A](a: A)(implicit ev: Mix[A, Bar]): A with Bar = {
  ev.mix(a, new Bar { def bar = 2 })
}
withBar[Foo](new Foo()).bar // 2
```
Definitions in the second type will override implementations in the first type.

## ZIO Modules
One of the primary motivations for writing this library was more comfortable incremental building
of ZIO environment cakes. A possible way of doing this is:
```scala
import zio.delegate._
import zio.blocking.Blocking
import zio.clock.Clock

trait Sys extends Serializable {
  def sys: Sys.Service[Any]
}
object Sys {
  trait Service[R] extends Serializable

  trait Live extends Sys { self: Clock with Blocking =>
    def sys = new Service[Any] {}
  }

  def withSys[A <: Clock with Blocking](a: A)(implicit ev: A Mix Sys): A with Sys = {
    class SysInstance(@delegate underlying: Clock with Blocking) extends Live
    // alternatively
    // class SysInstance(underlying: Clock with Blocking) extends Live with Clock with Blocking {
    //   val blocking = underlying.blocking
    //   val clock = underlying.clock
    // }
    ev.mix(a, new SysInstance(a))
  }
}
```
## Remarks
This is heavily inspired by both [adamw/scala-macro-aop](https://github.com/adamw/scala-macro-aop) and [b-studios/MixinComposition](https://github.com/b-studios/MixinComposition). Make sure to check out the projects!
