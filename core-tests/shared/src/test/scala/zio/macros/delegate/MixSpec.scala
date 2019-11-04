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
import zio.test.{ DefaultRunnableSpec, assert, suite, testM }
import zio.test.Assertion.equalTo

import MixSuite._

object MixSpec
    extends DefaultRunnableSpec(
      suite("Mix")(
        testM("should allow mixing of traits")(e1),
        testM("should overwrite methods defined on both instances with the second")(e2),
        testM("should allow the first type to be a class")(e3),
        testM("should support methods with same name")(e4),
        suite("should support type aliases")(
          testM("case 1")(e5),
          testM("case 2")(e6)
        ),
        testM("should support type arguments")(e7),
        testM("should allow overriding members")(e8)
      )
    )

object MixSuite {

  val e1 = {
    trait Foo {
      def a: Int = 1
    }
    trait Bar {
      def b: Int = 2
    }

    for {
      left  <- UIO(new Foo {})
      right <- UIO(new Bar {})
      mixed <- UIO(Mix[Foo, Bar].mix(left, right))
    } yield assert(mixed.a, equalTo(1)) && assert(mixed.b, equalTo(2))
  }

  val e2 = {
    trait Foo {
      def a: Int = 1
    }
    trait Bar extends Foo {
      override def a = 2
    }

    for {
      left  <- UIO(new Foo {})
      right <- UIO(new Bar {})
      mixed <- UIO(Mix[Foo, Bar].mix(left, right))
    } yield assert(mixed.a, equalTo(2))
  }

  val e3 = {
    class Foo {
      def a: Int = 1
    }
    trait Bar {
      def b: Int = 2
    }

    for {
      left  <- UIO(new Foo())
      right <- UIO(new Bar {})
      mixed <- UIO(Mix[Foo, Bar].mix(left, right))
    } yield assert(mixed.a, equalTo(1)) && assert(mixed.b, equalTo(2))
  }

  val e4 = {
    trait Foo {
      def a(a: Int): Int
    }
    trait Bar {
      def a(a: String): String
    }

    for {
      left <- UIO(new Foo {
               def a(a: Int) = 1
             })
      right <- UIO(new Bar {
                def a(a: String) = "foo"
              })
      mixed <- UIO(Mix[Foo, Bar].mix(left, right))
    } yield assert(mixed.a(1), equalTo(1)) && assert(mixed.a(""), equalTo("foo"))
  }

  val e5 = {
    trait Foo {
      def a(a: Int): Int
    }
    trait Bar {
      def a(a: String): String
    }
    trait Baz
    type FooBar = Foo with Bar

    for {
      left <- UIO(new Foo with Bar {
               def a(a: Int)    = 2
               def a(a: String) = "foo"
             })
      right <- UIO(new Baz {})
      mixed <- UIO(Mix[FooBar, Baz].mix(left, right))
    } yield assert(mixed.a(1), equalTo(2)) && assert(mixed.a(""), equalTo("foo"))
  }

  val e6 = {
    trait Foo {
      def a(a: Int): Int
    }
    trait Bar {
      def a(a: String): String
    }
    trait Baz
    type FooBar = Foo with Bar

    for {
      left <- UIO(new Baz {})
      right <- UIO(new Foo with Bar {
                def a(a: Int)    = 2
                def a(a: String) = "foo"
              })
      mixed <- UIO(Mix[Baz, FooBar].mix(left, right))
    } yield assert(mixed.a(1), equalTo(2)) && assert(mixed.a(""), equalTo("foo"))
  }

  val e7 = {
    trait Foo[A] {
      def a(a: Int): A
    }
    trait Bar {
      def b(a: String): String
    }
    trait Baz
    type FooBar = Foo[Int] with Bar

    for {
      left <- UIO(new Baz {})
      right <- UIO(new Foo[Int] with Bar {
                def a(a: Int)    = 2
                def b(a: String) = "foo"
              })
      mixed <- UIO(Mix[Baz, FooBar].mix(left, right))
    } yield assert(mixed.a(1), equalTo(2)) && assert(mixed.b(""), equalTo("foo"))
  }

  val e8 = {
    trait Foo {
      def a: Int = 1
    }
    trait Bar

    for {
      left <- UIO(new Foo with Bar {})
      right <- UIO(new Foo {
                override def a = 2
              })
      mixed <- UIO(Mix[Foo with Bar, Foo].mix(left, right))
    } yield assert(mixed.a, equalTo(2))
  }
}
