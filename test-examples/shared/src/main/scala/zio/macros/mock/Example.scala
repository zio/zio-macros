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
import zio.test.mock.Method

@mockable
trait Example {

  val example: Example.Service[Any]
}

object Example {

  val preValue: Int = 42

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

  val postValue: Int = 42
}

object ValidateMockable {
  // if macro expands correctly code below should compile
  val get: Method[Int, String]                  = Example.get
  val set: Method[(Int, String), Unit]          = Example.set
  val getAndSet: Method[(Int, String), String]  = Example.getAndSet
  val getAndSet2: Method[(Int, String), String] = Example.getAndSet2
  val clear: Method[Unit, Unit]                 = Example.clear
  val clear2: Method[Unit, Unit]                = Example.clear2
  val clear3: Method[Unit, Unit]                = Example.clear3
  val overloaded0: Method[Int, String]          = Example.overloaded._0
  val overloaded1: Method[Long, String]         = Example.overloaded._1
  val rio: Method[Unit, String]                 = Example.rio
  val urio: Method[Unit, String]                = Example.urio
  val nonAbstract: Method[Int, String]          = Example.nonAbstract

  val preValue: Int  = Example.preValue
  val postValue: Int = Example.postValue
}
