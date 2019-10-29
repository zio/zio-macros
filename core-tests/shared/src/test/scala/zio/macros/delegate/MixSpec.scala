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

import zio.test.{ DefaultRunnableSpec, assert, suite, test }
import zio.test.Assertion.equalTo

object MixSpec
    extends DefaultRunnableSpec(
      suite("Mix")(
        test("should allow mixing of traits") {
          trait Foo {
            def a: Int = 1
          }
          trait Bar {
            def b: Int = 2
          }
          val mixed = Mix[Foo, Bar].mix(new Foo {}, new Bar {})
          assert(mixed.a, equalTo(1)) && assert(mixed.b, equalTo(2))
        },
        test("should overwrite methods defined on both instances with the second") {
          trait Foo {
            def a: Int = 1
          }
          trait Bar extends Foo {
            override def a = 2
          }
          val mixed = Mix[Foo, Bar].mix(new Foo {}, new Bar {})
          assert(mixed.a, equalTo(2))
        },
        test("should allow the first type to be a class") {
          class Foo {
            def a: Int = 1
          }
          trait Bar {
            def b: Int = 2
          }
          val mixed = Mix[Foo, Bar].mix(new Foo(), new Bar {})
          assert(mixed.a, equalTo(1)) && assert(mixed.b, equalTo(2))
        },
        test("should support methods with same name") {
          trait Foo {
            def a(a: Int): Int
          }
          trait Bar {
            def a(a: String): String
          }
          val mixed = Mix[Foo, Bar].mix(new Foo { def a(a: Int) = 1 }, new Bar { def a(a: String) = "foo" })
          assert(mixed.a(1), equalTo(1)) && assert(mixed.a(""), equalTo("foo"))
        },
        suite("should support type aliases")(
          test("case 1") {
            trait Foo {
              def a(a: Int): Int
            }
            trait Bar {
              def a(a: String): String
            }
            trait Baz
            type FooBar = Foo with Bar
            val mixed =
              Mix[FooBar, Baz].mix(new Foo with Bar { def a(a: Int) = 2; def a(a: String) = "foo" }, new Baz {})
            assert(mixed.a(1), equalTo(2)) && assert(mixed.a(""), equalTo("foo"))
          },
          test("case 2") {
            trait Foo {
              def a(a: Int): Int
            }
            trait Bar {
              def a(a: String): String
            }
            trait Baz
            type FooBar = Foo with Bar
            val mixed = Mix[Baz, FooBar].mix(new Baz {}, new Foo with Bar {
              def a(a: Int) = 2; def a(a: String) = "foo"
            })
            assert(mixed.a(1), equalTo(2)) && assert(mixed.a(""), equalTo("foo"))
          }
        ),
        test("should support type arguments") {
          trait Foo[A] {
            def a(a: Int): A
          }
          trait Bar {
            def b(a: String): String
          }
          trait Baz
          type FooBar = Foo[Int] with Bar
          val mixed =
            Mix[Baz, FooBar].mix(new Baz {}, new Foo[Int] with Bar { def a(a: Int) = 2; def b(a: String) = "foo" })
          assert(mixed.a(1), equalTo(2)) && assert(mixed.b(""), equalTo("foo"))
        },
        test("should allow overriding members") {
          trait Foo {
            def a: Int = 1
          }
          trait Bar
          val mixed = Mix[Foo with Bar, Foo].mix(new Foo with Bar {}, new Foo { override def a = 2 })
          assert(mixed.a, equalTo(2))
        }
      )
    )
