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

import zio.{ RIO, UManaged, URIO, ZIO }
import zio.macros.annotation.mockable
import zio.test.Assertion.equalTo
import zio.test.mock.Expectation.{ unit, value }

@mockable
trait Module {

  val module: Module.Service[Any]
}

object Module {

  trait Service[R] {

    def get(id: Int): ZIO[R, Nothing, String]
    def set(id: Int, value: String): ZIO[R, Nothing, Unit]
    def getAndSet(id: Int, value: String): ZIO[R, Nothing, String]
    def getAndSet2(id: Int)(value: String): ZIO[R, Nothing, String]
    def clear(): ZIO[R, Nothing, Unit]
    def clear2: ZIO[R, Nothing, Unit]
    val clear3: ZIO[R, Nothing, Unit]
    def overloaded(value: Int): ZIO[R, Nothing, String]
    def overloaded(value: Long): ZIO[R, Nothing, String]
    val rio: RIO[R, String]
    val urio: URIO[R, String]
    def nonAbstract(id: Int): ZIO[R, Nothing, String] = get(id)
  }
}

object MockableExample {

  val mockModule: UManaged[Module] =
    for {
      _ <- Module.get(equalTo(1)).returns(value("foo"))
      _ <- Module.set(equalTo(1 -> "foo")).returns(unit)
      _ <- Module.getAndSet(equalTo(1 -> "foo")).returns(value("foo"))
      _ <- Module.getAndSet2(equalTo(1 -> "foo")).returns(value("foo"))
      _ <- Module.clear.returns(unit)
      _ <- Module.clear2.returns(unit)
      _ <- Module.clear3.returns(unit)
      _ <- Module.overloaded._0(equalTo(1)).returns(value("foo"))
      _ <- Module.overloaded._1(equalTo(1L)).returns(value("foo"))
      _ <- Module.rio.returns(value("foo"))
      _ <- Module.urio.returns(value("foo"))
      _ <- Module.nonAbstract(equalTo(1)).returns(value("foo"))
    } yield ()
}
