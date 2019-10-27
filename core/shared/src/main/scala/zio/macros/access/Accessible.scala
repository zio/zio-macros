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

import zio.macros.core.ModulePattern

import scala.annotation.{ StaticAnnotation, compileTimeOnly }
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable macro paradise to expand macro annotations")
class accessible() extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AccessibleMacro.apply
}

private[access] class AccessibleMacro(val c: Context) extends ModulePattern {
  import c.universe._

  def apply(annottees: c.Tree*): c.Tree = {
    val trees       = extractTrees(annottees)
    val module      = extractModule(trees.module)
    val companion   = extractCompanion(trees.companion)
    val service     = extractService(companion.body)
    val capabilites = extractCapabilities(service)
    val accessors   = generateCapabilityAccessors(trees.module, module.serviceName, capabilites)
    val updated     = generateUpdatedCompanion(module, companion, accessors)

    q"""
       ${trees.module}
       $updated
     """
  }

  private def generateCapabilityAccessors(
    module: ClassDef,
    serviceName: TermName,
    capabilities: List[Capability]
  ): List[Tree] =
    capabilities.map { capability =>
      val moduleType   = module.name
      val (name, e, a) = (capability.name, capability.error, capability.value)
      val mods =
        if (capability.impl == EmptyTree) Modifiers()
        else Modifiers(Flag.OVERRIDE)

      val returnType = tq"_root_.zio.ZIO[$moduleType, $e, $a]"
      val returnValue =
        capability.argLists match {
          case Some(argLists) if argLists.flatten.nonEmpty =>
            val argNames = argLists.map(_.map(_.name))
            q"_root_.zio.ZIO.accessM(_.$serviceName.$name(...$argNames))"
          case _ =>
            q"_root_.zio.ZIO.accessM(_.$serviceName.$name)"
        }

      capability.argLists match {
        case None            => q"$mods val $name: $returnType = $returnValue"
        case Some(Nil)       => q"$mods def $name: $returnType = $returnValue"
        case Some(List(Nil)) => q"$mods def $name(): $returnType = $returnValue"
        case Some(argLists)  => q"$mods def $name(...$argLists): $returnType = $returnValue"
      }
    }

  private def generateUpdatedCompanion(
    module: ModuleSummary,
    companion: CompanionSummary,
    capabilityAccessors: List[Tree]
  ): Tree =
    q"""
      object ${companion.name} {

        ..${companion.body}

       object > extends Service[${module.name}] {
         ..$capabilityAccessors
       }
      }
    """
}
