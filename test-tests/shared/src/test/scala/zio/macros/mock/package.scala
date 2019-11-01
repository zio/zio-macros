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

import zio.{ RIO, URIO, ZIO }

/**
 * When defined test modules directly in the suite the compiler kept failing with error:
 *    an unexpected type representation reached the compiler backend while compiling MockableSpec.scala: <notype>. If possible, please file a bug on https://github.com/scala/bug/issues.
 *    Error while emitting MockableSpec.scala
 *    <notype> (of class scala.reflect.internal.Types$NoType$)
 *
 * TODO: investigate why and if we can move these pack into the suites
 */
@mockable
trait Foo01  { val foo01: Foo01.Service[Any] }
object Foo01 { trait Service[R] { val a: ZIO[R, Nothing, Unit] } }

@mockable
trait Foo02  { val foo02: Foo02.Service[Any] }
object Foo02 { trait Service[R] { def a: ZIO[R, Nothing, Unit] } }

@mockable
trait Foo03  { val foo03: Foo03.Service[Any] }
object Foo03 { trait Service[R] { def a(): ZIO[R, Nothing, Unit] } }

@mockable
trait Foo04  { val foo04: Foo04.Service[Any] }
object Foo04 { trait Service[R] { def a(v1: Int): ZIO[R, Nothing, Unit] } }

@mockable
trait Foo05  { val foo05: Foo05.Service[Any] }
object Foo05 { trait Service[R] { def a(v1: Int, v2: Int): ZIO[R, Nothing, Unit] } }

@mockable
trait Foo06  { val foo06: Foo06.Service[Any] }
object Foo06 { trait Service[R] { def a(v1: Int)(v2: Int): ZIO[R, Nothing, Unit] } }

@mockable
trait Foo07 { val foo07: Foo07.Service[Any] }

object Foo07 {

  trait Service[R] {
    def a(v1: Int): ZIO[R, Nothing, Unit]
    def a(v1: Long): ZIO[R, Nothing, Unit]
  }
}

@mockable
trait Foo08  { val foo08: Foo08.Service[Any] }
object Foo08 { trait Service[R] { val a: RIO[R, Unit] } }

@mockable
trait Foo09  { val foo09: Foo09.Service[Any] }
object Foo09 { trait Service[R] { val a: URIO[R, Unit] } }

@mockable
trait Foo10  { val foo10: Foo10.Service[Any] }
object Foo10 { trait Service[R] { val a: ZIO[R, Nothing, Unit] = ZIO.unit } }

@mockable
trait Foo11 { val foo11: Foo11.Service[Any] }

object Foo11 {
  val preValue: Int = 42
  trait Service[R] { def a: ZIO[R, Nothing, Unit] }
  val postValue: Int = 42
}
