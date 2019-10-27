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
package zio.macros.core

import scala.reflect.macros.whitebox.Context
import scala.util.Try

private[macros] trait ZIOExtractor {

  val c: Context

  import c.universe._

  object ZIO {

    def unapply(tree: AppliedTypeTree): Option[(Tree, Tree, Tree)] =
      for {
        tree    <- if (tree.args.length <= dummies.length) Some(tree) else None
        replace = dummies.zip(tree.args).toMap
        typeArgs <- scala.util.Try {
                     appliedType(c.typecheck(tree.tpt, c.TYPEmode).tpe.dealias.typeConstructor, replace.keys.toList).dealias.typeArgs.map {
                       t =>
                         replace.get(t).getOrElse(tq"$t")
                     }
                   }.toOption
        result <- typeArgs match {
                   case r :: e :: a :: Nil => Some((r, e, a))
                   case _                  => None
                 }
      } yield result

    private[this] case object Dummy1
    private[this] case object Dummy2
    private[this] case object Dummy3
    private[this] case object Dummy4
    private[this] case object Dummy5
    private[this] case object Dummy6
    private[this] case object Dummy7
    private[this] case object Dummy8
    private[this] case object Dummy9

    private[this] val dummies = List(
      weakTypeOf[Dummy1.type],
      weakTypeOf[Dummy2.type],
      weakTypeOf[Dummy3.type],
      weakTypeOf[Dummy4.type],
      weakTypeOf[Dummy5.type],
      weakTypeOf[Dummy6.type],
      weakTypeOf[Dummy7.type],
      weakTypeOf[Dummy8.type],
      weakTypeOf[Dummy9.type]
    )
  }
}
