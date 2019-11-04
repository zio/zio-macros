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
package zio.macros.delegate

import zio.UIO
import zio.test.Assertion.equalTo
import zio.test.{ DefaultRunnableSpec, assert, suite, testM }
import zio.test.TestAspect.ignore

object DelegateSpec
    extends DefaultRunnableSpec(
      suite("delegate annotation")(
        DelegateSuite.automaticallyExtendTraits,
        DelegateSuite.allowOverrides,
        DelegateSuite.handleFinalMethods,
        DelegateSuite.extendingAbstractClasses,
        DelegateSuite.methodsWithSameName,
        DelegateSuite.typeParameters
      )
    )

object DelegateSuite {

  val automaticallyExtendTraits = testM("should automatically extend traits") {
    trait Foo {
      def a: Int
    }

    for {
      inner <- UIO(new Foo {
                def a = 3
              })
      outer <- UIO {
                class Bar(@delegate foo: Foo)
                new Bar(inner)
              }
    } yield assert(outer.a, equalTo(3))
  }

  val allowOverrides = testM("should allow overrides") {
    trait Foo {
      def a: Int = 3
    }

    for {
      inner <- UIO(new Foo {})
      outer <- UIO {
                class Bar(@delegate foo: Foo) {
                  override def a = 4
                }
                new Bar(inner)
              }
    } yield assert(outer.a, equalTo(4))
  }

  val handleFinalMethods = testM("should handle final methods") {
    trait Foo {
      final def a: Int = 3
    }

    for {
      inner <- UIO(new Foo {})
      outer <- UIO {
                class Bar(@delegate foo: Foo) extends Foo
                new Bar(inner)
              }
    } yield assert(outer.a, equalTo(3))
  }

  /*
  // TODO: this is an example that triggers this issue https://github.com/milessabin/shapeless/issues/614
  val locallyVisibleSymbols = testM("should handle locally visible symbols") {
    object Test {
      trait Foo {
        final def a: Int = 3
      }
    }

    for {
      inner <- UIO(new Test.Foo {})
      outer <- UIO {
                class Bar(@delegate(verbose = true) foo: Test.Foo)
                new Bar(inner)
              }
    } yield assert(outer.a, equalTo(3))
  }
   */

  val extendingAbstractClasses = testM("should work with abstract classes when explicitly extending") {
    abstract class Foo {
      def a: Int
    }

    for {
      inner <- UIO(new Foo {
                def a = 3
              })
      outer <- UIO {
                class Bar(@delegate foo: Foo) extends Foo
                new Bar(inner)
              }
    } yield assert(outer.a, equalTo(3))
  }

  val methodsWithSameName = suite("should handle methods with same name but different signatures")(
    testM("case 1") {
      trait Foo {
        def a(i: Int): Int = 3
      }

      for {
        inner <- UIO(new Foo {})
        outer <- UIO {
                  class Bar(@delegate foo: Foo) {
                    def a(s: String) = "bar"
                  }
                  new Bar(inner)
                }
      } yield assert(outer.a(""), equalTo("bar")) && assert(outer.a(0), equalTo(3))
    } @@ ignore, // TODO: Fails on 2.11 with ClassCastException
    testM("case 2") {
      trait Foo {
        def a(i: Int): Int = 3
      }
      trait Foo1 extends Foo {
        def a(s: String) = "bar"
      }

      for {
        inner <- UIO(new Foo1 {})
        outer <- UIO {
                  class Bar(@delegate foo: Foo1)
                  new Bar(inner)
                }
      } yield assert(outer.a(""), equalTo("bar")) && assert(outer.a(0), equalTo(3))
    }
  )

  val typeParameters = testM("should handle type parameters") {
    trait Foo[A] {
      def a: A
    }
    trait Bar extends Foo[Int] {
      def a: Int = 1
    }

    for {
      inner <- UIO(new Bar {})
      outer <- UIO {
                class Baz(@delegate bar: Bar)
                new Baz(inner)
              }
    } yield assert(outer.a, equalTo(1))
  }

  /*
  // TODO: test case for https://github.com/zio/zio-macros/issues/17
  val typeParametersOnResultingClass = testM("should handle type parameters on resulting class") {
    trait Foo[A] {
      def a: A
    }

    for {
      inner <- UIO(new Foo[Int] {
        val a = 1
      })
      outer <- UIO {
        class Bar[A](@delegate foo: Foo[A])
        new Bar[Int[(inner)
      }
    } yield assert(outer.a, equalTo(1))
  }
 */
}
