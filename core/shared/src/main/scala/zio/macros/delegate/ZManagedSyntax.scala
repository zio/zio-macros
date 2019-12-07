package zio.macros.delegate

import zio._

trait ZManagedSyntax {

  implicit class ZManagedOps[R, E, A](zManaged: ZManaged[R, E, A]) {

    def providePart[R1]: ProvidePartZManaged[R1, R, E, A] =
      new ProvidePartZManaged(zManaged)

    def @@[B](enrichWith: EnrichWith[B])(implicit ev: A Mix B): ZManaged[R, E, A with B] =
      enrichWith.enrichZManaged[R, E, A](zManaged)

    def @@[B](enrichWithM: EnrichWithM[A, E, B])(implicit ev: A Mix B): ZManaged[R, E, A with B] =
      enrichWithM.enrichZManaged[R, E, A](zManaged)

    def @@[B](enrichWithManaged: EnrichWithManaged[A, E, B])(implicit ev: A Mix B): ZManaged[R, E, A with B] =
      enrichWithManaged.enrichZManaged[R, E, A](zManaged)
  }

}
