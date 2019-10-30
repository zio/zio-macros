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
package zio.macros.access

import zio.{ RIO, URIO, ZIO }

@accessible
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

object ValidateAccessible {
  // if macro expands correctly code below should compile
  def get(id: Int): URIO[Example, String]                       = Example.>.get(id)
  def set(id: Int, value: String): URIO[Example, Unit]          = Example.>.set(id, value)
  def getAndSet(id: Int, value: String): URIO[Example, String]  = Example.>.getAndSet(id, value)
  def getAndSet2(id: Int)(value: String): URIO[Example, String] = Example.>.getAndSet2(id)(value)
  def clear(): URIO[Example, Unit]                              = Example.>.clear()
  def clear2: URIO[Example, Unit]                               = Example.>.clear2
  val clear3: URIO[Example, Unit]                               = Example.>.clear3
  def overloaded(value: Int): URIO[Example, String]             = Example.>.overloaded(value)
  def overloaded(value: Long): URIO[Example, String]            = Example.>.overloaded(value)
  val rio: RIO[Example, String]                                 = Example.>.rio
  val urio: URIO[Example, String]                               = Example.>.urio
  def nonAbstract(id: Int): URIO[Example, String]               = Example.>.nonAbstract(id)

  val preValue: Int  = Example.preValue
  val postValue: Int = Example.postValue
}
