package zio.macros.delegate

import zio._
import zio.clock.Clock
import zio.random.Random
import zio.duration.Duration

trait ZScheduleSyntax {

  implicit class ZScheduleSyntax[R, A, B](zSchedule: ZSchedule[R, A, B]) {

    def delayed$[R1 <: R with Clock](f: Duration => Duration)(implicit ev: R Mix Clock): ZSchedule[R1, A, B] =
      zSchedule.delayedEnv(f)(g => patch[R, Clock].apply(c => new Clock { val clock = g(c.clock) }))

    def delayedM$[R1 <: R with Clock](
      f: Duration => ZIO[R1, Nothing, Duration]
    )(implicit ev: R Mix Clock): ZSchedule[R1, A, B] =
      zSchedule.delayedMEnv(f)(g => patch[R, Clock].apply(c => new Clock { val clock = g(c.clock) }))

    def jittered$[R1 <: R with Clock with Random](min: Double = 0.0, max: Double = 1.0)(
      implicit ev: R Mix Clock
    ): ZSchedule[R1, A, B] =
      zSchedule.jitteredEnv(min, max)(f => patch[R, Clock].apply(c => new Clock { val clock = f(c.clock) }))

    def modifyDelay$[R1 <: R with Clock](
      f: (B, Duration) => ZIO[R1, Nothing, Duration]
    )(implicit ev: R Mix Clock): ZSchedule[R1, A, B] =
      zSchedule.modifyDelayEnv(f)(g => patch[R, Clock].apply(c => new Clock { val clock = g(c.clock) }))
  }

}
