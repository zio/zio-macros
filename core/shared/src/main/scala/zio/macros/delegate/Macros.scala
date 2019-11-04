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

import com.github.ghik.silencer.silent
import scala.reflect.macros.blackbox.Context
import scala.reflect.macros.TypecheckException

private[macros] class Macros(val c: Context) {
  import c.universe._

  case class MethodSummary(
    method: MethodSymbol,
    methodType: Type
  ) {

    def returnType: Type = methodType match {
      case m: MethodType        => m.resultType
      case m: NullaryMethodType => m.resultType
      case _                    => abort("not a method")
    }

    def typeParams: List[Symbol] = methodType match {
      case m: MethodType        => m.typeParams
      case m: NullaryMethodType => m.typeParams
      case _                    => abort("not a method")
    }

    def paramLists: List[List[Symbol]] = methodType match {
      case m: MethodType        => m.paramLists
      case m: NullaryMethodType => m.paramLists
      case _                    => abort("not a method")
    }
  }

  def mixImpl[A: WeakTypeTag, B: WeakTypeTag]: c.Tree = {
    val aTT = weakTypeOf[A]
    val bTT = weakTypeOf[B]

    val bTTComps = getTypeComponents(bTT) // we need do this because refinements does not count as a trait
    val aTTComps = getTypeComponents(aTT)

    // aT may extend a class
    //bT may not as it will be mixed in
    preconditions(
      (!aTT.typeSymbol.isFinal -> s"${aTT.typeSymbol.toString()} must be nonfinal class or trait.") ::
        bTTComps.map(t => t.typeSymbol.asClass.isTrait -> s"${t.typeSymbol.toString()} needs to be a trait."): _*
    )

    val aName = TermName(c.freshName("a"))
    val bName = TermName(c.freshName("b"))
    val (resultTypeName, resultTypeTree) = (aTTComps ++ bTTComps).distinct
      .foldLeft[Option[(TypeName, Tree)]](None) {
        case (Some((n, acc)), t) =>
          val symbol = TypeName(c.freshName("tmp"))
          val next   = q"""
          ..$acc
          trait $symbol extends $n with $t
        """
          Some((symbol, next))
        case (_, t) =>
          val symbol = TypeName(c.freshName("tmp"))
          val next   = q"trait $symbol extends $t"
          Some((symbol, next))
      }
      .get
    val resultType = typeCheckTree(q"""
      ..${resultTypeTree}
      null.asInstanceOf[${resultTypeName}]
    """).fold(e => abort(e.toString), identity)

    val body = {
      val methods = overlappingMethods(aTT, resultType).map((_, aName)).toMap ++
        overlappingMethods(bTT, resultType).map((_, bName)).toMap

      methods.filterNot { case (m, _) => isObjectMethod(m.method) }.map {
        case (m, owner) => delegateMethodDef(m, owner)
      }
    }
    q"""
    ..${resultTypeTree}
    new _root_.zio.macros.delegate.Mix[$aTT, $bTT] {
      def mix($aName: $aTT, $bName: $bTT) = {
        new ${resultTypeName} {
          ..$body
        }
      }
    }
    """
  }

  @silent("pattern var [^\\s]+ in method unapply is never used")
  def delegateImpl(annottees: c.Expr[Any]*): c.Tree = {

    case class Arguments(verbose: Boolean, forwardObjectMethods: Boolean, generateTraits: Boolean)

    val args: Arguments = c.prefix.tree match {
      case Apply(_, args) =>
        val verbose: Boolean = args.collectFirst {
          case q"verbose = $cfg" =>
            c.eval(c.Expr[Boolean](cfg))
        }.getOrElse(false)
        val forwardObjectMethods = args.collectFirst {
          case q"forwardObjectMethods = $cfg" =>
            c.eval(c.Expr[Boolean](cfg))
        }.getOrElse(false)
        val generateTraits = args.collectFirst {
          case q"generateTraits = $cfg" =>
            c.eval(c.Expr[Boolean](cfg))
        }.getOrElse(true)
        Arguments(verbose, forwardObjectMethods, generateTraits)
      case other => abort("not possible - macro invoked on type that does not have @delegate: " + showRaw(other))
    }

    def isBlackListed(m: MethodSymbol) =
      if (!args.forwardObjectMethods) isObjectMethod(m) else false

    def modifiedClass(classDecl: ClassDef, delegateTo: ValDef): c.Tree = {
      val q"..$mods class $className(..$fields) extends ..$bases { ..$body }" = classDecl
      val existingMethods = body
        .flatMap(
          tree =>
            tree match {
              case DefDef(_, n, _, _, _, _) => Some(n)
              case ValDef(_, n, _, _)       => Some(n)
              case _                        => None
            }
        )
        .toSet

      val (toName, toType) = typeCheckVal(delegateTo)
        .fold(e => abort(s"Failed typechecking annotated member. Is it defined in local scope?: $e"), identity)

      val basesTypes = bases.map(b => c.typecheck(b, c.TYPEmode).tpe)
      val additionalTraits =
        if (args.generateTraits) {
          val baseTraitsOnly = basesTypes.flatMap(getTraits(_).map(_.toType)).toList
          getTraits(toType)
            .map(_.toType)
            .filterNot { tpe =>
              baseTraitsOnly.exists(tpe =:= _)
            }
            .toList
        } else Nil

      val (resultType, resultTypeName) = {
        val resultTypes = basesTypes ++ additionalTraits
        (internal.refinedType(resultTypes, internal.newScopeWith()), resultTypes)
      }

      val extensions = overlappingMethods(toType, resultType, !isBlackListed(_))
        .filterNot(m => existingMethods.contains(m.method.name))
        .map(delegateMethodDef(_, toName))

      q"""
      $mods class $className(..$fields) extends ..$resultTypeName { ..${body ++ extensions} }
      """
    }

    annottees.map(_.tree) match {
      case (valDecl: ValDef) :: (classDecl: ClassDef) :: Nil =>
        preconditions(
          (classDecl.tparams.isEmpty) -> "Classes with generic parameters are not currently supported."
        )
        val modified = modifiedClass(classDecl, valDecl)
        if (args.verbose) showInfo(modified.toString())
        modified
      case _ => abort("Invalid annottee")
    }
  }

  final private[this] def delegateMethodDef(m: MethodSummary, to: TermName) = {
    val name  = m.method.name
    val rType = m.returnType
    val mods  = if (!m.method.isAbstract) Modifiers(Flag.OVERRIDE) else Modifiers()

    if (m.method.paramLists.isEmpty) {
      q"$mods val $name: $rType = $to.$name"
    } else {
      val typeParams = m.typeParams.map(internal.typeDef(_))
      val paramLists = m.paramLists.map(_.map(internal.valDef(_)))
      q"""
      $mods def $name[..${typeParams}](...$paramLists): $rType = {
        $to.${name}(...${paramLists.map(_.map(_.name))})
      }
      """
    }
  }

  final private[this] def isObjectMethod(m: MethodSymbol): Boolean =
    Set(
      "java.lang.Object.clone",
      "java.lang.Object.hashCode",
      "java.lang.Object.finalize",
      "java.lang.Object.equals",
      "java.lang.Object.toString",
      "scala.Any.getClass"
    ).contains(m.fullName)

  final private[this] def getTraits(t: Type): Set[ClassSymbol] = {
    def loop(stack: List[ClassSymbol], traits: Vector[ClassSymbol] = Vector()): Vector[ClassSymbol] = stack match {
      case x :: xs =>
        loop(xs, if (x.isTrait && t <:< x.toType) traits :+ x else traits)
      case Nil => traits
    }
    loop(t.baseClasses.map(_.asClass)).toSet
  }

  @silent("method right in class Either is deprecated")
  final private[this] val typeCheckVal: ValDef => Either[TypecheckException, (TermName, Type)] = {
    case ValDef(_, tname, tpt, _) =>
      typeCheckTree(tpt).right.map((tname, _))
  }

  final private[this] def typeCheckTree(tree: Tree): Either[TypecheckException, Type] =
    try {
      Right(c.typecheck(tree, c.TYPEmode).tpe)
    } catch {
      case e: TypecheckException => Left(e)
    }

  @silent("method enclosing(Class|Package) in trait Enclosures is deprecated")
  final private[this] val enclosing: String = c.enclosingClass match {
    case clazz if clazz.isEmpty => c.enclosingPackage.symbol.fullName
    case clazz                  => clazz.symbol.fullName
  }

  final private[this] def overlappingMethods(
    interface: Type,
    impl: Type,
    filter: MethodSymbol => Boolean = _ => true
  ): Set[MethodSummary] = {
    def isVisible(m: MethodSymbol) =
      m.isPublic || enclosing.startsWith(m.privateWithin.fullName)

    interface.members
      .flatMap(m => impl.member(m.name).alternatives.map(_.asMethod).find(_ == m))
      .filter(m => !m.isConstructor && !m.isFinal && isVisible(m) && filter(m))
      .map(m => MethodSummary(m, m.typeSignatureIn(interface)))
      .toSet
  }

  final private[this] def showInfo(s: String) =
    c.info(c.enclosingPosition, s.split("\n").mkString("\n |---macro info---\n |", "\n |", ""), true)

  final private[this] def abort(s: String) =
    c.abort(c.enclosingPosition, s)

  final private[this] def preconditions(conds: (Boolean, String)*): Unit =
    conds.foreach {
      case (cond, s) =>
        if (!cond) abort(s)
    }

  final private[this] def getTypeComponents(t: Type): List[Type] = t.dealias match {
    case RefinedType(parents, _) => parents.flatMap(p => getTypeComponents(p))
    case t                       => List(t)
  }
}
