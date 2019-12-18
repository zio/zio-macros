package zio.macros.delegate

import zio._

trait ZIOSyntax {

  implicit class ZIOOps[R, E, A](zio: ZIO[R, E, A]) {

    def providePart[R1]: ProvidePartZIO[R1, R, E, A] =
      new ProvidePartZIO(zio)

    def @@[B](enrichWith: EnrichWith[B])(implicit ev: A Mix B): ZIO[R, E, A with B] =
      enrichWith.enrichZIO[R, E, A](zio)

    def @@[E1 >: E, B](enrichWithM: EnrichWithM[A, E1, B])(implicit ev: A Mix B): ZIO[R, E1, A with B] =
      enrichWithM.enrichZIO[R, E1, A](zio)

    def @@[E1 >: E, B](
      enrichWithManaged: EnrichWithManaged[A, E1, B]
    )(implicit ev: A Mix B): ZManaged[R, E1, A with B] =
      enrichWithManaged.enrichZManaged[R, E1, A](zio.toManaged_)
  }

}
