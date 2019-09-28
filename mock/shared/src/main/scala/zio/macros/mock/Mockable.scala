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
package zio.macros.mock

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Mockable() extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro MockableMacro.apply
}

private[mock] class MockableMacro(val c: Context) {
  import c.universe._

  case class TreesSummary(
    module: ClassDef,
    companion: ModuleDef
  )

  case class ModuleSummary(
    previousSiblings: List[Tree],
    nextSiblings: List[Tree],
    mods: Modifiers,
    name: TypeName,
    typeParams: List[TypeDef],
    earlyDefinitions: List[Tree],
    parents: List[Tree],
    self: ValDef,
    body: List[Tree],
    serviceName: TermName
  )

  case class CompanionSummary(
    mods: Modifiers,
    name: TermName,
    earlyDefinitions: List[Tree],
    parents: List[Tree],
    self: ValDef,
    body: List[Tree]
  )

  case class ServiceSummary(
    previousSiblings: List[Tree],
    service: Tree,
    nextSiblings: List[Tree],
    mods: Modifiers,
    typeParams: List[TypeDef],
    earlyDefinitions: List[Tree],
    parents: List[Tree],
    self: ValDef,
    body: List[Tree]
  )

  def apply(annottees: c.Tree*): c.Tree = {
    val trees     = extractTrees(annottees)
    val module    = extractModule(trees.module)
    val companion = extractCompanion(trees.companion)
    val service   = extractService(companion.body)
    val tags      = generateCapabilityTags(module, companion, service)
    val mocks     = generateCapabilityMocks(module, companion, service)
    val updated   = generateUpdatedCompanion(module, companion, service, tags, mocks)

    q"""
       ${trees.module}
       $updated
     """
  }

  private def extractTrees(annottees: Seq[c.Tree]): TreesSummary =
    annottees match {
      case (module: ClassDef) :: (companion: ModuleDef) :: Nil if module.name.toTermName == companion.name  =>
        TreesSummary(module, companion)
      case (companion: ModuleDef) :: (module: ClassDef) :: Nil if module.name.toTermName == companion.name  =>
        TreesSummary(module, companion)
      case _ => abort("Module trait and companion object pair not found")
    }

  private def extractModule(module: ClassDef): ModuleSummary =
    module match {
      case q"$mods trait $moduleName[..$typeParams] extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
        body.indexWhere {
          case ValDef(mods, name, typeTree, rhs) =>
            val moduleTerm = moduleName.toTermName
            name.toString.capitalize == moduleTerm.toString && typeTree.toString == s"$moduleTerm.Service[Any]"
          case _ => false
        } match {
          case idx if idx >= 0 =>
            val (prevSiblings, service :: nextSiblings) = body.splitAt(idx)
            ModuleSummary(prevSiblings, nextSiblings, mods, moduleName, typeParams, earlyDefinitions, parents, self, body, service.asInstanceOf[ValDef].name.toTermName)
          case _ => abort("Service value not found in module trait")
        }
      case _ => abort("Could not extract module trait")
    }

  private def extractCompanion(companion: ModuleDef): CompanionSummary =
    companion match {
      case q"$mods object $name extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
        CompanionSummary(mods, name, earlyDefinitions, parents, self, body)
      case _ => abort("Count not extract module companion")
    }

  private def extractService(body: List[Tree]): ServiceSummary =
    body.indexWhere {
      case ClassDef(mods, name, typeParams, implementation) => name.toTermName.toString == "Service"
      case _ => false
    } match {
      case idx if idx >= 0 =>
        val (prevSiblings, service :: nextSiblings) = body.splitAt(idx)
        service match {
          case q"$mods trait Service[..$typeParams] extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
            ServiceSummary(prevSiblings, service, nextSiblings, mods, typeParams, earlyDefinitions, parents, self, body)
          case _ => abort("Could not extract service trait")
        }
      case _ => abort("Could not find service trait")
    }

  private def generateCapabilityTags(module: ModuleSummary, companion: CompanionSummary, service: ServiceSummary): List[Tree] =
    service.body.flatMap {

      case DefDef(_, termName, _, argLists, returns: AppliedTypeTree, _) if isZIO(returns) =>
        val inputType = argLists.flatten match {
          case Nil => tq"Nothing"
          case arg :: Nil => arg.tpt
          case args =>
            if (args.size > 22) abort(s"Unable to generate capability tag for method $termName with more than 22 arguments.")
            val typeParams = args.map(_.tpt)
            tq"(..$typeParams)"
        }
        val outputType = returns.args.last
        Some(q"case object $termName extends _root_.zio.test.mock.Method[$inputType, $outputType]")

      case ValDef(_, termName, returns: AppliedTypeTree, _) if isZIO(returns) =>
        val inputType = tq"Nothing"
        val outputType = returns.args.last
        Some(q"case object $termName extends _root_.zio.test.mock.Method[$inputType, $outputType]")

      case _ => None
    }

  private def generateCapabilityMocks(module: ModuleSummary, companion: CompanionSummary, service: ServiceSummary): List[Tree] =
    service.body.flatMap {

      case DefDef(_, termName, _, argLists, returns: AppliedTypeTree, _) if isZIO(returns) =>
        val (e :: a :: Nil) = returns.args.tail
        argLists.flatten match {

          case Nil =>
            Some(q"def $termName(...$argLists): _root_.zio.IO[$e, $a] = mock(Service.$termName)")

          case args =>
            val argNames = args.map(_.name)
            Some(q"def $termName(...$argLists): _root_.zio.IO[$e, $a] = mock(Service.$termName)(..$argNames)")
        }

      case ValDef(_, termName, returns: AppliedTypeTree, _) if isZIO(returns) =>
        val (e :: a :: Nil) = returns.args.tail
        Some(q"val $termName: _root_.zio.IO[$e, $a] = mock(Service.$termName)")

      case _ => None
    }

  private def generateUpdatedCompanion(module: ModuleSummary, companion: CompanionSummary, service: ServiceSummary, capabilityTags: List[Tree], capabilityMocks: List[Tree]): Tree =
    q"""
      object ${companion.name} {

        ..${service.previousSiblings}

       trait Service[R] {
         ..${service.body}
       }

       object Service {
         ..$capabilityTags
       }

      implicit val mockable: _root_.zio.test.mock.Mockable[${module.name}] = (mock: _root_.zio.test.mock.Mock) =>
        new ${module.name} {
          val ${module.serviceName} = new Service[Any] {
            ..$capabilityMocks
          }
        }

        ..${service.nextSiblings}
      }
    """

  private def isZIO(returns: AppliedTypeTree): Boolean =
    returns match {
      case AppliedTypeTree(Ident(term), typeParams) => term.toString == "ZIO" && typeParams.size == 3
      case _ => false
    }

  private def abort(details: String) = {
    val error = "The annotation can only be used on ZIO modules (see https://zio.dev/docs/overview/overview_module_pattern)."
    c.abort(c.enclosingPosition, s"$error $details.")
  }
}
