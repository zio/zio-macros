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

import com.github.ghik.silencer.silent
import scala.reflect.macros.whitebox.Context

private[macros] trait ModulePattern {

  val c: Context

  import c.universe._

  private[this] class ZIOExtractor(typeParams: List[TypeDef]) {

    def unapply(tree: AppliedTypeTree): Option[(Type, Type, Type)] = {
      val typeName = TypeName(c.freshName())
      c.typecheck(tq"({ type $typeName[..${typeParams}] = $tree })#$typeName", c.TYPEmode).tpe.dealias.typeArgs match {
        case r :: e :: a :: Nil => Some((r, e, a))
        case _                  => None
      }
    }
  }

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
    mods: Modifiers,
    argLists: Option[List[List[ValDef]]],
    env: Type,
    error: Type,
    value: Type,
    impl: Tree
  )

  protected def extractTrees(annottees: Seq[c.Tree]): TreesSummary =
    annottees match {
      case (module: ClassDef) :: (companion: ModuleDef) :: Nil if module.name.toTermName == companion.name =>
        TreesSummary(module, companion)
      case (companion: ModuleDef) :: (module: ClassDef) :: Nil if module.name.toTermName == companion.name =>
        TreesSummary(module, companion)
      case _ => abort("Module trait and companion object pair not found")
    }

  @silent("pattern var [^\\s]+ in method unapply is never used")
  protected def extractModule(module: ClassDef): ModuleSummary =
    module match {
      case q"$mods trait $moduleName[..$typeParams] extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
        body.indexWhere {
          case ValDef(_, name, typeTree, _) =>
            val moduleTerm = moduleName.toTermName
            name.toString.capitalize == moduleTerm.toString && typeTree.toString == s"$moduleTerm.Service[Any]"
          case _ => false
        } match {
          case idx if idx >= 0 =>
            val (prevSiblings, service, nextSiblings) = siblings(body, idx)
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

  @silent("pattern var [^\\s]+ in method unapply is never used")
  protected def extractCompanion(companion: ModuleDef): CompanionSummary =
    companion match {
      case q"$mods object $name extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
        CompanionSummary(mods, name, earlyDefinitions, parents, self, body)
      case _ => abort("Count not extract module companion")
    }

  @silent("pattern var [^\\s]+ in method unapply is never used")
  protected def extractService(body: List[Tree]): ServiceSummary =
    body.indexWhere {
      case ClassDef(_, name, _, _) => name.toTermName.toString == "Service"
      case _                       => false
    } match {
      case idx if idx >= 0 =>
        val (prevSiblings, service, nextSiblings) = siblings(body, idx)
        service match {
          case q"$mods trait Service[..$typeParams] extends { ..$earlyDefinitions } with ..$parents { $self => ..$body }" =>
            ServiceSummary(prevSiblings, service, nextSiblings, mods, typeParams, earlyDefinitions, parents, self, body)
          case _ => abort("Could not extract service trait")
        }
      case _ => abort("Could not find service trait")
    }

  protected def extractCapabilities(service: ServiceSummary): List[Capability] = {
    val zio = new ZIOExtractor(service.typeParams)
    service.body.collect {
      case DefDef(mods, termName, _, argLists, zio(r, e, a), impl) =>
        Capability(termName, mods, Some(argLists), r, e, a, impl)

      case ValDef(mods, termName, zio(r, e, a), impl) =>
        Capability(termName, mods, None, r, e, a, impl)
    }
  }

  @silent("match may not be exhaustive")
  protected def siblings(list: List[Tree], idx: Int): (List[Tree], Tree, List[Tree]) = {
    val (prev, at :: next) = list.splitAt(idx)
    (prev, at, next)
  }

  protected def abort(details: String) = {
    val error =
      "The annotation can only applied to modules following the module pattern (see https://zio.dev/docs/howto/howto_use_module_pattern)."
    c.abort(c.enclosingPosition, s"$error $details.")
  }
}
