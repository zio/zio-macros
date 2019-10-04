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

private[macros] trait ModulePattern {

  val c: Context

  import c.universe._

  protected case class TreesSummary(
    module: ClassDef,
    companion: ModuleDef
  )

  protected case class ModuleSummary(
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

  protected case class CompanionSummary(
    mods: Modifiers,
    name: TermName,
    earlyDefinitions: List[Tree],
    parents: List[Tree],
    self: ValDef,
    body: List[Tree]
  )

  protected case class ServiceSummary(
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

  protected case class Capability(
    name: TermName,
    argLists: Option[List[List[ValDef]]],
    env: Tree,
    error: Tree,
    value: Tree
  )

  protected def extractTrees(annottees: Seq[c.Tree]): TreesSummary =
    annottees match {
      case (module: ClassDef) :: (companion: ModuleDef) :: Nil if module.name.toTermName == companion.name =>
        TreesSummary(module, companion)
      case (companion: ModuleDef) :: (module: ClassDef) :: Nil if module.name.toTermName == companion.name =>
        TreesSummary(module, companion)
      case _ => abort("Module trait and companion object pair not found")
    }

  protected def extractModule(module: ClassDef): ModuleSummary =
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
            ModuleSummary(
              prevSiblings,
              nextSiblings,
              mods,
              moduleName,
              typeParams,
              earlyDefinitions,
              parents,
              self,
              body,
              service.asInstanceOf[ValDef].name.toTermName
            )
          case _ => abort("Service value not found in module trait")
        }
      case _ => abort("Could not extract module trait")
    }

  protected def extractCompanion(companion: ModuleDef): CompanionSummary =
    companion match {
      case q"$mods object $name extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
        CompanionSummary(mods, name, earlyDefinitions, parents, self, body)
      case _ => abort("Count not extract module companion")
    }

  protected def extractService(body: List[Tree]): ServiceSummary =
    body.indexWhere {
      case ClassDef(mods, name, typeParams, implementation) => name.toTermName.toString == "Service"
      case _                                                => false
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

  protected def extractCapabilities(service: ServiceSummary): List[Capability] =
    service.body.collect {
      case DefDef(_, termName, _, argLists, AppliedTypeTree(Ident(term), r :: e :: a :: Nil), _)
          if term.toString == "ZIO" =>
        Capability(termName, Some(argLists), r, e, a)

      case ValDef(_, termName, AppliedTypeTree(Ident(term), r :: e :: a :: Nil), _) if term.toString == "ZIO" =>
        Capability(termName, None, r, e, a)
    }

  protected def abort(details: String) = {
    val error =
      "The annotation can only applied to modules following the module pattern (see https://zio.dev/docs/howto/howto_use_module_pattern)."
    c.abort(c.enclosingPosition, s"$error $details.")
  }
}
