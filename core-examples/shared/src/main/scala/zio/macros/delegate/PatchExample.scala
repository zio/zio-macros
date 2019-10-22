package zio.macros.delegate

import zio.clock.Clock

object PatchExample {

  type ZEnv = zio.DefaultRuntime#Environment

  val mapClock: (Clock.Service[Any] => Clock.Service[Any]) => ZEnv => ZEnv =
    f => patch[ZEnv, Clock].apply(c => new Clock { val clock = f(c.clock) })
}
