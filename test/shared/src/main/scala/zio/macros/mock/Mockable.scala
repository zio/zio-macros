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

import zio.macros.core.ModulePattern

import scala.annotation.{ StaticAnnotation, compileTimeOnly }
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable macro paradise to expand macro annotations")
class mockable() extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro MockableMacro.apply
}

private[mock] class MockableMacro(val c: Context) extends ModulePattern {
  import c.universe._

  def apply(annottees: c.Tree*): c.Tree = {
    val trees       = extractTrees(annottees)
    val module      = extractModule(trees.module)
    val companion   = extractCompanion(trees.companion)
    val service     = extractService(companion.body)
    val capabilites = extractCapabilities(service)
    val tags        = generateCapabilityTags(module, capabilites)
    val mocks       = generateCapabilityMocks(companion, capabilites)
    val updated     = generateUpdatedCompanion(module.name, module.name.toTermName, module.serviceName, service, tags, mocks)

    q"""
       ${trees.module}
       $updated
     """
  }

  private def generateCapabilityTags(
    module: ModuleSummary,
    capabilities: List[Capability]
  ): List[Tree] =
    capabilities
      .groupBy(_.name)
      .collect {
        case (name, capability :: Nil) =>
          generateCapabilityTag(module, name, capability)
        case (name, overloads) =>
          val body: List[Tree] = overloads.zipWithIndex.map {
            case (capability, idx) =>
              val idxName = TermName(s"_$idx")
              generateCapabilityTag(module, idxName, capability)
          }

          q"object $name { ..$body }"
      }
      .toList

  private def generateCapabilityTag(
    module: ModuleSummary,
    name: TermName,
    capability: Capability
  ): Tree = {
    val moduleType = module.name
    val inputType = capability.argLists.map(_.flatten).getOrElse(Nil) match {
      case Nil        => tq"Unit"
      case arg :: Nil => arg.tpt
      case args =>
        if (args.size > 22) abort(s"Unable to generate capability tag for method $name with more than 22 arguments.")
        val typeParams = args.map(_.tpt)
        tq"(..$typeParams)"
    }
    val outputType = capability.value
    q"case object $name extends _root_.zio.test.mock.Method[$moduleType, $inputType, $outputType]"
  }

  private def generateCapabilityMocks(
    companion: CompanionSummary,
    capabilities: List[Capability]
  ): List[Tree] =
    capabilities
      .groupBy(_.name)
      .collect {
        case (_, capability :: Nil) =>
          List(generateCapabilityMock(companion, capability, None))
        case (_, overloads) =>
          overloads.zipWithIndex.map {
            case (capability, idx) =>
              val idxName = TermName(s"_$idx")
              generateCapabilityMock(companion, capability, Some(idxName))
          }
      }
      .toList
      .flatten

  private def generateCapabilityMock(
    companion: CompanionSummary,
    capability: Capability,
    overloadIndex: Option[TermName]
  ): Tree = {
    val (name, e, a) = (capability.name, capability.error, capability.value)
    val tag = overloadIndex match {
      case Some(index) => q"${companion.name}.${capability.name}.$index"
      case None        => q"${companion.name}.${capability.name}"
    }

    val mods =
      if (capability.impl == EmptyTree) Modifiers(Flag.FINAL)
      else Modifiers(Flag.FINAL | Flag.OVERRIDE)

    val returnType = tq"_root_.zio.IO[$e, $a]"
    val returnValue =
      capability.argLists match {
        case Some(argLists) if argLists.flatten.nonEmpty =>
          val argNames = argLists.flatten.map(_.name)
          q"mock($tag, ..$argNames)"
        case _ =>
          q"mock($tag)"
      }

    capability.argLists match {
      case None            => q"$mods val $name: $returnType = $returnValue"
      case Some(Nil)       => q"$mods def $name: $returnType = $returnValue"
      case Some(List(Nil)) => q"$mods def $name(): $returnType = $returnValue"
      case Some(argLists)  => q"$mods def $name(...$argLists): $returnType = $returnValue"
    }
  }

  private def generateUpdatedCompanion(
    moduleType: TypeName,
    moduleName: TermName,
    serviceName: TermName,
    service: ServiceSummary,
    capabilityTags: List[Tree],
    capabilityMocks: List[Tree]
  ): Tree =
    q"""
       object $moduleName {

         ..${service.previousSiblings}

         trait Service[R] {
           ..${service.body}
         }

         ..$capabilityTags

         implicit val mockable: _root_.zio.test.mock.Mockable[$moduleType] = (mock: _root_.zio.test.mock.Mock) =>
           new $moduleType {
             val $serviceName = new Service[Any] {
               ..$capabilityMocks
             }
           }

         ..${service.nextSiblings}
       }
     """
}
