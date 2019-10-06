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
    val tags        = generateCapabilityTags(capabilites)
    val mocks       = generateCapabilityMocks(capabilites)
    val updated     = generateUpdatedCompanion(module, companion, service, tags, mocks)

    q"""
       ${trees.module}
       $updated
     """
  }

  private def generateCapabilityTags(capabilities: List[Capability]): List[Tree] =
    capabilities
      .groupBy(_.name)
      .collect {
        case (name, capability :: Nil) =>
          generateCapabilityTag(name, capability)
        case (name, overloads) =>
          val body: List[Tree] = overloads.zipWithIndex.map {
            case (capability, idx) =>
              val idxName = TermName(s"_$idx")
              generateCapabilityTag(idxName, capability)
          }

          q"object $name { ..$body }"
      }
      .toList

  private def generateCapabilityTag(name: TermName, capability: Capability): Tree = {
    val inputType = capability.argLists.map(_.flatten).getOrElse(Nil) match {
      case Nil        => tq"Unit"
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
    capabilities
      .groupBy(_.name)
      .collect {
        case (_, capability :: Nil) =>
          List(generateCapabilityMock(capability, None))
        case (_, overloads) =>
          overloads.zipWithIndex.map {
            case (capability, idx) =>
              val idxName = TermName(s"_$idx")
              generateCapabilityMock(capability, Some(idxName))
          }
      }
      .toList
      .flatten

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

  private def generateUpdatedCompanion(
    module: ModuleSummary,
    companion: CompanionSummary,
    service: ServiceSummary,
    capabilityTags: List[Tree],
    capabilityMocks: List[Tree]
  ): Tree =
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
}
