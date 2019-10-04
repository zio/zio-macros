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

  case class Capability(
    name: TermName,
    argLists: Option[List[List[ValDef]]],
    env: Tree,
    error: Tree,
    value: Tree
  )

  def apply(annottees: c.Tree*): c.Tree = {
    val trees       = extractTrees(annottees)
    val module      = extractModule(trees.module)
    val companion   = extractCompanion(trees.companion)
    val service     = extractService(companion.body)
    val capabilites = extractCapabilities(service)
    val tags        = generateCapabilityTags(capabilites)
    val mocks       = generateCapabilityMocks(capabilites)
    val updated     = generateUpdatedCompanion(module, companion, service, tags, mocks)

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

  private def extractCapabilities(service: ServiceSummary): List[Capability] =
    service.body.collect {
      case DefDef(_, termName, _, argLists, AppliedTypeTree(Ident(term), r :: e :: a :: Nil), _) if term.toString == "ZIO" =>
        Capability(termName, Some(argLists), r, e, a)

      case ValDef(_, termName, AppliedTypeTree(Ident(term), r :: e :: a :: Nil), _) if term.toString == "ZIO" =>
        Capability(termName, None, r, e, a)
    }

  private def generateCapabilityTags(capabilities: List[Capability]): List[Tree] =
    capabilities.groupBy(_.name).collect {
      case (name, capability :: Nil) =>
        generateCapabilityTag(name, capability)
      case (name, overloads) =>
        val body: List[Tree] = overloads.zipWithIndex.map {
          case (capability, idx) =>
            val idxName = TermName(s"_$idx")
            generateCapabilityTag(idxName, capability)
        }

        q"object $name { ..$body }"
    }.toList

  private def generateCapabilityTag(name: TermName, capability: Capability): Tree = {
    val inputType = capability.argLists.map(_.flatten).getOrElse(Nil) match {
      case Nil => tq"Unit"
      case arg :: Nil => arg.tpt
      case args =>
        if (args.size > 22) abort(s"Unable to generate capability tag for method $name with more than 22 arguments.")
        val typeParams = args.map(_.tpt)
        tq"(..$typeParams)"
    }
    val outputType = capability.value
    q"case object $name extends _root_.zio.test.mock.Method[$inputType, $outputType]"
  }

  private def generateCapabilityMocks(capabilities: List[Capability]): List[Tree] =
    capabilities.groupBy(_.name).collect {
      case (name, capability :: Nil) =>
        List(generateCapabilityMock(capability, None))
      case (name, overloads) =>
        overloads.zipWithIndex.map {
          case (capability, idx) =>
            val idxName = TermName(s"_$idx")
            generateCapabilityMock(capability, Some(idxName))
        }
    }.toList.flatten

  private def generateCapabilityMock(capability: Capability, overloadIndex: Option[TermName]): Tree = {
    val tag = overloadIndex match {
      case Some(index) => q"Service.${capability.name}.$index"
      case None        => q"Service.${capability.name}"
    }

    capability match {
      case Capability(name, None, _, e, a) =>
        q"val $name: _root_.zio.IO[$e, $a] = mock($tag)"
      case Capability(name, Some(Nil), _, e, a) =>
        q"def $name: _root_.zio.IO[$e, $a] = mock($tag)"
      case Capability(name, Some(List(Nil)), _, e, a) =>
        q"def $name(): _root_.zio.IO[$e, $a] = mock($tag)"
      case Capability(name, Some(argLists), _, e, a) =>
        val argNames = argLists.flatten.map(_.name)
        q"def $name(...$argLists): _root_.zio.IO[$e, $a] = mock($tag, ..$argNames)"
    }
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

  private def abort(details: String) = {
    val error = "The annotation can only applied to modules following the module pattern (see https://zio.dev/docs/howto/howto_use_module_pattern)."
    c.abort(c.enclosingPosition, s"$error $details.")
  }
}
