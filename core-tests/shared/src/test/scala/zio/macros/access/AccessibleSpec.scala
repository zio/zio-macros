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
package zio.macros.access

import zio.{ RIO, UIO, URIO, ZIO }
import zio.test.{ DefaultRunnableSpec, assert, suite, test, testM }
import zio.test.Assertion.{ anything, equalTo }

object AccessibleSpec
    extends DefaultRunnableSpec(
      suite("accessible annotation")(
        suite("style")(
          testM("trait") {
            @accessible("trait")
            trait Foo  { val foo: Foo.Service[Any] }
            object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

            for {
              makeAccessors <- UIO(() => new Foo.Accessors {})
              accessors     <- UIO(makeAccessors())
            } yield assert(accessors, anything)
          },
          suite("alias")(
            test(">") {
              @accessible(">")
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

              assert(Foo.>, anything)
            },
            test("alias") {
              @accessible("alias")
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

              assert(Foo.>, anything)
            }
          ),
          suite("default")(
            test("with parenthesis") {
              @accessible()
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

              assert(Foo.accessors, anything)
            },
            test("without parenthesis") {
              @accessible
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

              assert(Foo.accessors, anything)
            }
          )
        ),
        suite("should generate accessors for")(
          test("val") {
            @accessible()
            trait Foo  { val foo: Foo.Service[Any] }
            object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

            assert(Foo.accessors.a, anything)
          },
          suite("def")(
            suite("no args")(
              test("without parenthesis") {
                @accessible()
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a: ZIO[R, Nothing, Unit] } }

                assert(Foo.accessors.a, anything)
              },
              test("with parenthesis") {
                @accessible()
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a(): ZIO[R, Nothing, Unit] } }

                assert(Foo.accessors.a(), anything)
              }
            ),
            test("single argument") {
              @accessible()
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { def a(v1: Int): ZIO[R, Nothing, Unit] } }

              assert(Foo.accessors.a(1), anything)
            },
            suite("multiple arguments")(
              test("single parameter list") {
                @accessible()
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a(v1: Int, v2: Int): ZIO[R, Nothing, Unit] } }

                assert(Foo.accessors.a(1, 2), anything)
              },
              test("multiple parameter lists") {
                @accessible()
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { def a(v1: Int)(v2: Int): ZIO[R, Nothing, Unit] } }

                assert(Foo.accessors.a(1)(2), anything)
              }
            ),
            test("overloaded") {
              @accessible()
              trait Foo { val foo: Foo.Service[Any] }
              object Foo {
                trait Service[R] {
                  def a(v1: Int): ZIO[R, Nothing, Unit]
                  def a(v1: Long): ZIO[R, Nothing, Unit]
                }
              }

              assert(Foo.accessors.a(1), anything) && assert(Foo.accessors.a(1L), anything)
            },
            suite("zio aliases")(
              test("RIO") {
                @accessible()
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { val a: RIO[R, Unit] } }

                assert(Foo.accessors.a, anything)
              },
              test("URIO") {
                @accessible()
                trait Foo  { val foo: Foo.Service[Any] }
                object Foo { trait Service[R] { val a: URIO[R, Unit] } }

                assert(Foo.accessors.a, anything)
              }
            ),
            test("non abstract methods") {
              @accessible()
              trait Foo  { val foo: Foo.Service[Any] }
              object Foo { trait Service[R] { val a: ZIO[R, Nothing, Unit] = ZIO.unit } }

              assert(Foo.accessors.a, anything)
            }
          )
        ),
        test("should keep user-defined code") {
          @accessible()
          trait Foo { val foo: Foo.Service[Any] }
          object Foo {
            val preValue: Int = 42
            trait Service[R] {}
            val postValue: Int = 42
          }

          assert(Foo.preValue, equalTo(42)) && assert(Foo.postValue, equalTo(42))
        }
      )
    )
