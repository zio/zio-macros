# ZIO-MACROS

[![CircleCI][badge-ci]][link-ci]
[![Gitter][badge-gitter]][link-gitter]

Scap boilerplate in your ZIO projects.
Learn more about ZIO at:

 - [Homepage](https://zio.dev)

## Accessable

Generates helpers to access service capabilities from anywhere.

To use simply add `"dev.zio" %% "zio-macros-access" % "<version>"` to your `libraryDependencies`.

The `@Accessable` annotation can be used on _modules_ following the [module pattern](https://zio.dev/docs/howto/howto_use_module_pattern).

When applied to the _module_ it will autogenerate the `>` container in module's compation object with helpers to access service capabilities:

```scala
import zio.macros.access.Accessable

@Accessable
trait Example {
  val example: Example.Service[Any]
}

object Example {
  trait Service[R] {
    def foo()                 : ZIO[R, Nothing, Unit]
    def bar(v1: Int, v2: Int) : ZIO[R, Nothing, Int]
    def baz(v1: Int)(v2: Int) : ZIO[R, Nothing, String]
  }

  // -- below code is autogenerated -- //
  object > extends Service[R] {
    def foo()                 = ZIO.accessM(_.example.foo)
    def bar(v1: Int, v2: Int) = ZIO.accessM(_.example.bar(v1, v2))
    def baz(v1: Int)(v2: Int) = ZIO.accessM(_.example.baz(v1)(v2))
  }
  // -- end of autogenerated code -- //
}
```

You can use these helpers to refer to service _capabilities_ like:

```scala
val myProgram =
  for {
    _ <- Example.>.foo
    _ <- Example.>.bar(1, 2)
  } yield ()
```

## Mockable

To use simply add `"dev.zio" %% "zio-macros-mock" % "<version>"` to your `libraryDependencies`.

The `@Mockable` annotation can be used on _modules_ following the [module pattern](https://zio.dev/docs/howto/howto_use_module_pattern).

When applied to the _module_ it will autogenerate the `Service` companion object with _capability tags_ and the mockable implementation:

```scala
import zio.macros.mock.Mockable

@Mockable
trait Example {
  val example: Example.Service[Any]
}

object Example {
  trait Service[R] {
    def foo()                 : ZIO[R, Nothing, Unit]
    def bar(v1: Int, v2: Int) : ZIO[R, Throwable, Int]
    def baz(v1: Int)(v2: Int) : ZIO[R, Nothing, String]
    def overloaded(v: Int)    : ZIO[R, Nothing, String]
    def overloaded(v: Long)   : ZIO[R, Nothing, String]
  }

  // -- below code is autogenerated -- //
  object Service {
    object foo extends zio.test.mock.Method[Unit, Unit]
    object bar extends zio.test.mock.Method[(Int, Int), Int]
    object baz extends zio.test.mock.Method[(Int, Int), String]
    object overloaded {
      object _0 extends zio.test.mock.Method[Int, String]
      object _1 extends zio.test.mock.Method[Long, String]
    }
  }

  implicit val mockable: zio.test.mock.Mockable[Example] = (mock: zio.test.mock.Mock) =>
    new Example {
      val example = new Service[Any] {
        def foo()                 : zio.IO[Nothing, Unit]   = mock(Service.foo)
        def bar(v1: Int, v2: Int) : zio.IO[Throwable, Int]  = mock(Service.bar, v1, v2)
        def baz(v1: Int)(v2: Int) : zio.IO[Nothing, String] = mock(Service.baz, v1, v2)
        def overloaded(v: Int)    : zio.IO[Nothing, String] = mock(Service.overloaded._0, v)
        def overloaded(v: Long)   : zio.IO[Nothing, String] = mock(Service.overloaded._1, v)
      }
    }
  // -- end of autogenerated code -- //
}
```

You can use the mockable implementation and _capability tags_ to create ad-hoc mocked services like:

```scala
import zio.Managed
import zio.test.Assertion.equalTo
import zio.test.mock.MockSpec

val mockEnv: Managed[Nothing, Example] = (
  MockSpec.expect_(Example.baz)(equalTo(4 -> 7)("The answer to life:") *>
  MockSpec.expect_(Example.bar)(equalTo(4 -> 2)(42)
)
```

You will find more detailed information about the ZIO Mocking framework in [howto section](https://zio.dev/docs/howto/howto_mock_services).

[badge-ci]: https://circleci.com/gh/zio/zio-macros/tree/master.svg?style=svg
[badge-gitter]: https://badges.gitter.im/ZIO/zio-macros.svg
[link-ci]: https://circleci.com/gh/zio/zio-macros/tree/master
[link-gitter]: https://gitter.im/ZIO/zio-macros?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
