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
package zio.macros.access

import scala.annotation.{compileTimeOnly, StaticAnnotation}
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable macro paradise to expand macro annotations")
class Accessable() extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AccessableMacro.apply
}

private[access] class AccessableMacro(val c: Context) {
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
    val accessors = generateCapabilityAccessors(module, companion, service)
    val updated   = generateUpdatedCompanion(module, companion, accessors)

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

  private def generateCapabilityAccessors(module: ModuleSummary, companion: CompanionSummary, service: ServiceSummary): List[Tree] =
    service.body.flatMap {

      case DefDef(_, termName, _, argLists, returns: AppliedTypeTree, _) if isZIO(returns) =>
        val paramLists = argLists.map(_.map(_.name))
        argLists.flatten match {

          case Nil =>
            Some(q"def $termName(...$argLists) = _root_.zio.ZIO.accessM(_.${module.serviceName}.$termName(...$paramLists))")

          case arg :: Nil =>
            Some(q"def $termName(...$argLists) = _root_.zio.ZIO.accessM(_.${module.serviceName}.$termName(...$paramLists))")

          case args =>
            Some(q"def $termName(...$argLists) = _root_.zio.ZIO.accessM(_.${module.serviceName}.$termName(...$paramLists))")
        }

      case ValDef(_, termName, returns: AppliedTypeTree, _) if isZIO(returns) =>
        Some(q"val $termName = _root_.zio.ZIO.accessM(_.${module.serviceName}.$termName)")

      case _ => None
    }

  private def generateUpdatedCompanion(module: ModuleSummary, companion: CompanionSummary, capabilityAccessors: List[Tree]): Tree =
    q"""
      object ${companion.name} {

        ..${companion.body}

       object > extends Service[${module.name}] {
         ..$capabilityAccessors
       }
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
