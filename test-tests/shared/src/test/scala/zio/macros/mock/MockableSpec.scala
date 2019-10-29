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
package zio.macros.mock

//import zio.{ RIO, URIO, ZIO }
//import zio.test.{ DefaultRunnableSpec, assert, suite, test }
//import zio.test.Assertion.{ anything, isSubtype }
//import zio.test.mock.Method
import zio.test.{ DefaultRunnableSpec, suite }

object MockableSPec
    extends DefaultRunnableSpec(
      suite("mockable annotation")(
        /*
TODO: figure out whats going on and fix the test suite

// [error] an unexpected type representation reached the compiler backend while compiling MockableSpec.scala: <notype>. If possible, please file a bug on https://github.com/scala/bug/issues.
// [error] Error while emitting MockableSpec.scala
// [error] <notype> (of class scala.reflect.internal.Types$NoType$)

        suite("should generate mock method for")(
          test("val") {
            @mockable
            trait Foo  { val foo: Foo.Service[Any] }
            object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

            {
              assert(Foo.a, anything)
            }
          },
          suite("def")(
            suite("no args")(
              test("without parenthesis") {
                @mockable
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a: ZIO[R, Nothing, Unit] } }

                assert(Foo.a, isSubtype[Method[Unit, Unit]](anything))
              },
              test("with parenthesis") {
                @mockable
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a(): ZIO[R, Nothing, Unit] } }

                assert(Foo.a, isSubtype[Method[Unit, Unit]](anything))
              }
            ),
            test("single argument") {
              @mockable
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { def a(v1: Int): ZIO[R, Nothing, Unit] } }

              assert(Foo.a, isSubtype[Method[Int, Unit]](anything))
            },
            suite("multiple arguments")(
              test("single parameter list") {
                @mockable
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a(v1: Int, v2: Int): ZIO[R, Nothing, Unit] } }

                assert(Foo.a, isSubtype[Method[(Int, Int), Unit]](anything))
              },
              test("multiple parameter lists") {
                @mockable
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a(v1: Int)(v2: Int): ZIO[R, Nothing, Unit] } }

                assert(Foo.a, isSubtype[Method[(Int, Int), Unit]](anything))
              }
            ),
            test("overloaded") {
              @mockable
              trait Foo { val foo: Foo.Service[Any] }
              object Foo {
                trait Service[R] {
                  def a(v1: Int): ZIO[R, Nothing, Unit]
                  def a(v1: Long): ZIO[R, Nothing, Unit]
                }
              }

              assert(Foo.a._0, isSubtype[Method[Int, Unit]](anything)) && assert(Foo.a._1, isSubtype[Method[Long, Unit]](anything))
            },
            suite("zio aliases")(
              test("RIO") {
                @mockable
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { val a: RIO[R, Unit] } }

                assert(Foo.a, isSubtype[Method[Unit, Unit]](anything))
              },
              test("URIO") {
                @mockable
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { val a: URIO[R, Unit] } }

                assert(Foo.a, isSubtype[Method[Unit, Unit]](anything))
              }
            ),
            test("non abstract methods") {
              @mockable
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] = ZIO.unit } }

              assert(Foo.a, isSubtype[Method[Unit, Unit]](anything))
            }
          )
        ),
        test("should keep user-defined code") {
          @mockable
          trait Foo { val foo: Foo.Service[Any] }
          object Foo {
            val preValue: Int = 42
            trait Service[R] { def a: ZIO[R, Nothing, Unit] }
            val postValue: Int = 42
          }

          assert(Foo.preValue, equalTo(42)) && assert(Foo.postValue, equalTo(42))
        }
       */
      )
    )
