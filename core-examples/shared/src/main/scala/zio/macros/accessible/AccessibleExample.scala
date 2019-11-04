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
package zio.macros.accessible

import zio.{ RIO, URIO, ZIO }
import zio.macros.annotation.accessible

@accessible
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

package object module extends Module.Accessors

object AccessibleExample {

  val program =
    for {
      _ <- module.get(1)
      _ <- module.set(1, "foo")
      _ <- module.getAndSet(1, "foo")
      _ <- module.getAndSet2(1)("foo")
      _ <- module.clear()
      _ <- module.clear2
      _ <- module.clear3
      _ <- module.overloaded(1)
      _ <- module.overloaded(1L)
      _ <- module.rio.orDie
      _ <- module.urio
      _ <- module.nonAbstract(1)
    } yield ()
}
