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
import zio.macros.annotation.delegate
import zio.test.Assertion.equalTo
import zio.test.{ DefaultRunnableSpec, assert, suite, testM }
import zio.test.TestAspect.ignore

import DelegateSuite._

object DelegateSpec
    extends DefaultRunnableSpec(
      suite("delegate annotation")(
        testM("should automatically extend traits")(e1),
        testM("should allow overrides")(e2),
        testM("should handle final methods")(e3),
        testM("should work with abstract classes when explicitly extending")(e4),
        suite("should handle methods with same name but different signatures")(
          testM("case 1")(e5) @@ ignore, // TODO: Fails on 2.11 with ClassCastException
          testM("case 2")(e6)
        ),
        testM("should handle type parameters")(e7)
        //, testM("should handle locally visible symbols")(e8)
        //, testM("should handle type parameters on resulting class")(e9)
      )
    )

object DelegateSuite {

  val e1 = {
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

  val e2 = {
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

  val e3 = {
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

  val e4 = {
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

  val e5 = {
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
  }

  val e6 = {
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

  val e7 = {
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
  // TODO: this is an example that triggers this issue https://github.com/milessabin/shapeless/issues/614
  val e8 = {
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
  // TODO: test case for https://github.com/zio/zio-macros/issues/17
  val e9 = {
    trait Foo[A] {
      def a: A
    }

    for {
      inner <- UIO(new Foo[Int] { val a = 1 })
      outer <- UIO {
                class Bar[A](@delegate foo: Foo[A])
                new Bar[Int](inner)
              }
    } yield assert(outer.a, equalTo(1))
  }
 */
}
