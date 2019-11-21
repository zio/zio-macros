package zio.macros.delegate

final class Extend[B](b: B) {
  def apply[A](a: A)(implicit ev: A Mix B): A with B = ev.mix(a, b)
}
