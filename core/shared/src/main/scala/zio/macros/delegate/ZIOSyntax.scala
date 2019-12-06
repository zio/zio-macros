package zio.macros.delegate

import zio._

trait ZIOSyntax {

  implicit class ZIOOps[R, E, A](zio: ZIO[R, E, A]) {

    def providePart[R1]: ProvidePartZIO[R1, R, E, A] =
      new ProvidePartZIO(zio)

    def @@[B](enrichWith: EnrichWith[B])(implicit ev: A Mix B): ZIO[R, E, A with B] =
      enrichWith.enrichZIO[R, E, A](zio)

    def @@[B](enrichWithM: EnrichWithM[A, E, B])(implicit ev: A Mix B): ZIO[R, E, A with B] =
      enrichWithM.enrichZIO[R, E, A](zio)

    def @@[B](enrichWithManaged: EnrichWithManaged[A, E, B])(implicit ev: A Mix B): ZManaged[R, E, A with B] =
      enrichWithManaged.enrichZManaged[R, E, A](zio.toManaged_)
  }

}
