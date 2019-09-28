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

import zio.ZIO
import zio.test.mock.Method

@Mockable
trait Sys {

  val sys: Sys.Service[Any]
}

object Sys {

  val preValue: Int = 42

  trait Service[R] {

    def get(id: Int): ZIO[R, Nothing, String]
    def set(id: Int, value: String): ZIO[R, Nothing, Unit]
    def getAndSet(id: Int, value: String): ZIO[R, Nothing, String]
    def getAndSet2(id: Int)(value: String): ZIO[R, Nothing, String]
    def clear(): ZIO[R, Nothing, Unit]
    def clear2: ZIO[R, Nothing, Unit]
    val clear3: ZIO[R, Nothing, Unit]
  }

  val postValue: Int = 42
}

object ValidateMockable {
  // if macro expands correctly code below should compile
  val get: Method[Int, String]                  = Sys.Service.get
  val set: Method[(Int, String), Unit]          = Sys.Service.set
  val getAndSet: Method[(Int, String), String]  = Sys.Service.getAndSet
  val getAndSet2: Method[(Int, String), String] = Sys.Service.getAndSet2
  val clear: Method[Nothing, Unit]              = Sys.Service.clear
  val clear2: Method[Nothing, Unit]             = Sys.Service.clear2
  val clear3: Method[Nothing, Unit]             = Sys.Service.clear3

  val preValue: Int  = Sys.preValue
  val postValue: Int = Sys.postValue
}
