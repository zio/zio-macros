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

final class EnrichWithM[-R, +E, A](private[this] val zio: ZIO[R, E, A]) {

  def enrichZIO[R1, E1 >: E, B <: R](that: ZIO[R1, E1, B])(implicit ev: B Mix A): ZIO[R1, E1, B with A] =
    that.flatMap(r1 => zio.provide(r1).map(ev.mix(r1, _)))

  def enrichZManaged[R1, E1 >: E, B <: R](that: ZManaged[R1, E1, B])(implicit ev: B Mix A): ZManaged[R1, E1, B with A] =
    that.flatMap(r1 => zio.provide(r1).map(ev.mix(r1, _)).toManaged_)
}

object EnrichWithM {
  final class PartiallyApplied[A] {
    def apply[R, E](zio: ZIO[R, E, A]): EnrichWithM[R, E, A] = new EnrichWithM(zio)
  }
}
