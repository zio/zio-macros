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

final class Eliminate[R](ext: Extend[R]) {

  def apply[R1, R2 >: R with R1, E, A](zio: ZIO[R2, E, A])(implicit ev: R1 Mix R): ZIO[R1, E, A] =
    zio.provideSome(r => ext[R1](r))

  def apply[R1, R2 >: R with R1, E, A](zManaged: ZManaged[R2, E, A])(implicit ev: R1 Mix R): ZManaged[R1, E, A] =
    zManaged.provideSome(r => ext[R1](r))

  def apply[R1, R2 >: R with R1, E, A](schedule: ZSchedule[R2, E, A])(implicit ev: R1 Mix R): ZSchedule[R1, E, A] =
    schedule.provideSome(r => ext[R1](r))

}
