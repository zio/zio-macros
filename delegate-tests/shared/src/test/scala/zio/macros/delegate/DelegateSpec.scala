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

class DelegateSpec extends UnitSpec {

  describe("delegate annotation") {
    it("should automatically extend traits") {
      trait Foo {
        def a: Int
      }
      {
        class Bar(@delegate foo: Foo)
        assert((new Bar(new Foo { def a = 3 })).a == 3)
      }
    }
    it("should allow overrides") {
      trait Foo {
        def a: Int = 3
      }
      {
        class Bar(@delegate foo: Foo) {
          override def a = 4
        }
        assert((new Bar(new Foo {})).a == 4)
      }
    }
    it("should handle final methods") {
      trait Foo {
        final def a: Int = 3
      }
      {
        class Bar(@delegate foo: Foo) extends Foo
        assert((new Bar(new Foo {})).a == 3)
      }
    }
    // TODO: this is an example that triggers this issue https://github.com/milessabin/shapeless/issues/614
    //     it("should handle locally visible symbols") {
    //       object Test {
    //         trait Foo {
    //           final def a: Int = 3
    //         }
    //       }
    //       {
    //         class Bar(@delegate(verbose = true) foo: Test.Foo)
    //         assert((new Bar(new Test.Foo {})).a == 3)
    //       }
    //     }
    it("should work with abstract classes when explicitly extending") {
      abstract class Foo {
        def a: Int
      }
      {
        class Bar(@delegate foo: Foo) extends Foo
        assert((new Bar(new Foo { def a = 3 })).a == 3)
      }
    }
    it("should handle methods with same name but different signatures - 1") {
      trait Foo {
        def a(i: Int): Int = 3
      }
      {
        class Bar(@delegate foo: Foo) {
          def a(s: String) = "bar"
        }
        val inst = new Bar(new Foo {})
        assert(inst.a("") == "bar" && inst.a(0) == 3)
      }
    }
    it("should handle methods with same name but different signatures - 2") {
      trait Foo {
        def a(i: Int): Int = 3
      }
      trait Foo1 extends Foo {
        def a(s: String) = "bar"
      }
      {
        class Bar(@delegate foo: Foo1)
        val inst = new Bar(new Foo1 {})
        assert(inst.a("") == "bar" && inst.a(0) == 3)
      }
    }
    it("should handle type parameters") {
      trait Foo[A] {
        def a: A
      }
      trait Bar extends Foo[Int] {
        def a: Int = 1
      }
      {
        class Baz(@delegate bar: Bar)
        assert((new Baz(new Bar {})).a == 1)
      }
    }
    // TODO: test case for https://github.com/zio/zio-macros/issues/17
    // it("should handle type parameters on resulting class") {
    //   trait Foo[A] {
    //     def a: A
    //   }
    //   {
    //     class Bar[A](@delegate foo: Foo[A])
    //     assert((new Bar[Int](new Foo[Int] {val a = 1})).a == 1)
    //   }
    // }
  }

}
