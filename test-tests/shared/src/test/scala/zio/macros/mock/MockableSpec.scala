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

import zio.test.{ DefaultRunnableSpec, assert, suite, test }
import zio.test.Assertion.{ anything, equalTo, isSubtype }
import zio.test.mock.Method

object MockableSPec
    extends DefaultRunnableSpec(
      suite("mockable annotation")(
        suite("should generate mock method for")(
          test("val") {
            assert(Foo01.a, anything)
          },
          suite("def")(
            suite("no args")(
              test("without parenthesis") {
                assert(Foo02.a, isSubtype[Method[Foo02, Unit, Unit]](anything))
              },
              test("with parenthesis") {
                assert(Foo03.a, isSubtype[Method[Foo03, Unit, Unit]](anything))
              }
            ),
            test("single argument") {
              assert(Foo04.a, isSubtype[Method[Foo04, Int, Unit]](anything))
            },
            suite("multiple arguments")(
              test("single parameter list") {
                assert(Foo05.a, isSubtype[Method[Foo05, (Int, Int), Unit]](anything))
              },
              test("multiple parameter lists") {
                assert(Foo06.a, isSubtype[Method[Foo06, (Int, Int), Unit]](anything))
              }
            ),
            test("overloaded") {
              assert(Foo07.a._0, isSubtype[Method[Foo07, Int, Unit]](anything)) && assert(
                Foo07.a._1,
                isSubtype[Method[Foo07, Long, Unit]](anything)
              )
            },
            suite("zio aliases")(
              test("RIO") {
                assert(Foo08.a, isSubtype[Method[Foo08, Unit, Unit]](anything))
              },
              test("URIO") {
                assert(Foo09.a, isSubtype[Method[Foo09, Unit, Unit]](anything))
              }
            ),
            test("non abstract methods") {
              assert(Foo10.a, isSubtype[Method[Foo10, Unit, Unit]](anything))
            }
          )
        ),
        test("should keep user-defined code") {
          assert(Foo11.preValue, equalTo(42)) && assert(Foo11.postValue, equalTo(42))
        }
      )
    )
