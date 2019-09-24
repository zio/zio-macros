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
    def clear: ZIO[R, Nothing, Unit]
    val clear2: ZIO[R, Nothing, Unit]
  }

  val postValue: Int = 42

}

object ValidateMockable {
  // if macro expands correctly code below should compile
  val a: Method[Int, String]           = Sys.Service.Get
  val b: Method[(Int, String), Unit]   = Sys.Service.Set
  val c: Method[(Int, String), String] = Sys.Service.GetAndSet
  val d: Method[(Int, String), String] = Sys.Service.GetAndSet2
  val e: Method[Nothing, Unit]         = Sys.Service.Clear
  val f: Method[Nothing, Unit]         = Sys.Service.Clear2
  val g: Int = Sys.preValue
  val h: Int = Sys.postValue
}
