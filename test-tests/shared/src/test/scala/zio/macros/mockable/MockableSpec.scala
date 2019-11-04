/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package zio.macros.mockable

import zio.{ RIO, URIO, ZIO }
import zio.macros.annotation.mockable
import zio.test.{ DefaultRunnableSpec, assert, suite, test }
import zio.test.Assertion.{ anything, equalTo, isSubtype }
import zio.test.mock.Method

import MockableSuite._

object MockableSpec
    extends DefaultRunnableSpec(
      suite("mockable annotation")(
        suite("should generate mock method for")(
          test("val")(e1),
          suite("def")(
            suite("no args")(
              test("without parenthesis")(e2),
              test("with parenthesis")(e3)
            ),
            test("single argument")(e4),
            suite("multiple arguments")(
              test("single parameter list")(e5),
              test("multiple parameter lists")(e6)
            ),
            test("overloaded")(e7),
            suite("zio aliases")(
              test("RIO")(e8),
              test("URIO")(e9)
            ),
            test("non abstract methods")(e10)
          )
        ),
        test("should keep user-defined code")(e11)
      )
    )

object MockableSuite {

  val e1 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

    assert(Foo.a, anything)
  }

  val e2 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { def a: ZIO[R, Nothing, Unit] } }

    assert(Foo.a, isSubtype[Method[Foo, Unit, Unit]](anything))
  }

  val e3 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { def a(): ZIO[R, Nothing, Unit] } }

    assert(Foo.a, isSubtype[Method[Foo, Unit, Unit]](anything))
  }

  val e4 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { def a(v1: Int): ZIO[R, Nothing, Unit] } }

    assert(Foo.a, isSubtype[Method[Foo, Int, Unit]](anything))
  }

  val e5 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { def a(v1: Int, v2: Int): ZIO[R, Nothing, Unit] } }
    assert(Foo.a, isSubtype[Method[Foo, (Int, Int), Unit]](anything))
  }

  val e6 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { def a(v1: Int)(v2: Int): ZIO[R, Nothing, Unit] } }

    assert(Foo.a, isSubtype[Method[Foo, (Int, Int), Unit]](anything))
  }

  val e7 = {
    @mockable
    trait Foo { val foo: Foo.Service[Any] }
    object Foo {
      trait Service[R] {
        def a(v1: Int): ZIO[R, Nothing, Unit]
        def a(v1: Long): ZIO[R, Nothing, Unit]
      }
    }

    assert(Foo.a._0, isSubtype[Method[Foo, Int, Unit]](anything)) && assert(
      Foo.a._1,
      isSubtype[Method[Foo, Long, Unit]](anything)
    )
  }

  val e8 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { val a: RIO[R, Unit] } }

    assert(Foo.a, isSubtype[Method[Foo, Unit, Unit]](anything))
  }

  val e9 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { val a: URIO[R, Unit] } }

    assert(Foo.a, isSubtype[Method[Foo, Unit, Unit]](anything))
  }

  val e10 = {
    @mockable
    trait Foo  { val foo: Foo.Service[Any] }
    object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] = ZIO.unit } }

    assert(Foo.a, isSubtype[Method[Foo, Unit, Unit]](anything))
  }

  val e11 = {
    @mockable
    trait Foo11 { val foo11: Foo11.Service[Any] }

    object Foo11 {
      val preValue: Int = 42
      trait Service[R] { def a: ZIO[R, Nothing, Unit] }
      val postValue: Int = 42
    }

    assert(Foo11.preValue, equalTo(42)) && assert(Foo11.postValue, equalTo(42))
  }
}
