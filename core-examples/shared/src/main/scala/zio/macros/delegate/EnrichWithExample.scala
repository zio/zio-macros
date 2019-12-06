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
import zio.clock.Clock
import zio.blocking.Blocking
import zio.console.Console
import zio.macros.delegate.syntax._

object EnrichWithExample {

  val blockingWithDeps: ZIO[Console, Throwable, Blocking] = ZIO.succeed(Blocking.Live)

  val enrichClock        = enrichWith[Clock](Clock.Live)
  val enrichClockM       = enrichWithM[Clock](ZIO.succeed(Clock.Live))
  val enrichClockManaged = enrichWithManaged[Clock](ZManaged.succeed(Clock.Live))

  val enrichBlocking          = enrichWith[Blocking](Blocking.Live)
  val enrichBlockingMWithDeps = enrichWithM[Blocking](blockingWithDeps)

  ZIO.succeed(new DefaultRuntime {}.environment) @@
    enrichClock @@
    enrichClock @@
    enrichClockM @@
    enrichClockManaged @@
    enrichClock @@
    enrichBlocking @@
    enrichBlockingMWithDeps
}
