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

import zio._

final class ProvidePartZIO[R1, R, E, A](zio: ZIO[R, E, A]) {

  def apply[R2, R3 >: R2 with R1 <: R](r1: R1)(implicit ev: R2 Mix R1): ZIO[R2, E, A] =
    zio.provideSome[R2] { r =>
      val r3: R3 = enrichWith[R1](r1)[R2](r)(ev)
      r3
    }

}
