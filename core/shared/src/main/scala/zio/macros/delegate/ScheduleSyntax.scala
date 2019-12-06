package zio.macros.delegate

import zio._
import zio.clock.Clock
import zio.random.Random
import zio.duration.Duration

trait ScheduleSyntax {

  implicit class ScheduleOps[R, A, B](schedule: Schedule[R, A, B]) {

    def providePart[R1]: ProvidePartSchedule[R1, R, A, B] =
      new ProvidePartSchedule(schedule)

    def delayed$[R1 <: R with Clock](f: Duration => Duration)(implicit ev: R Mix Clock): Schedule[R1, A, B] =
      schedule.delayedEnv(f)(g => patch[R, Clock].apply(c => new Clock { val clock = g(c.clock) }))

    def delayedM$[R1 <: R with Clock](
      f: Duration => ZIO[R1, Nothing, Duration]
    )(implicit ev: R Mix Clock): Schedule[R1, A, B] =
      schedule.delayedMEnv(f)(g => patch[R, Clock].apply(c => new Clock { val clock = g(c.clock) }))

    def jittered$[R1 <: R with Clock with Random](min: Double = 0.0, max: Double = 1.0)(
      implicit ev: R Mix Clock
    ): Schedule[R1, A, B] =
      schedule.jitteredEnv(min, max)(f => patch[R, Clock].apply(c => new Clock { val clock = f(c.clock) }))

    def modifyDelay$[R1 <: R with Clock](
      f: (B, Duration) => ZIO[R1, Nothing, Duration]
    )(implicit ev: R Mix Clock): Schedule[R1, A, B] =
      schedule.modifyDelayEnv(f)(g => patch[R, Clock].apply(c => new Clock { val clock = g(c.clock) }))
  }

}
