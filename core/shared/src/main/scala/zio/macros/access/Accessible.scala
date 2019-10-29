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

import com.github.ghik.silencer.silent
import zio.macros.core.ModulePattern

import scala.annotation.{ StaticAnnotation, compileTimeOnly }
import scala.language.experimental.macros
import scala.reflect.macros.whitebox.Context

@compileTimeOnly("enable macro paradise to expand macro annotations")
@silent("parameter value style in class accessible is never used")
class accessible(style: String = "default") extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AccessibleMacro.apply
}

private[access] class AccessibleMacro(val c: Context) extends ModulePattern {
  import c.universe._

  sealed trait Style
  object Style {
    case object Default   extends Style
    case object TraitOnly extends Style
    case object AliasOnly extends Style
  }

  def apply(annottees: c.Tree*): c.Tree = {

    @silent("""pattern var [^\s]+ in method unapply is never used""")
    val style: Style = c.prefix.tree match {
      case Apply(_, Literal(Constant(style)) :: Nil) =>
        style match {
          case "default"     => Style.Default
          case "trait"       => Style.TraitOnly
          case ">" | "alias" => Style.AliasOnly
          case _ => abort(s"Invalid style: $style")
        }
      case other => abort(s"Invalid macro call ${showRaw(other)}")
    }

    val trees            = extractTrees(annottees)
    val module           = extractModule(trees.module)
    val companion        = extractCompanion(trees.companion)
    val service          = extractService(companion.body)
    val capabilites      = extractCapabilities(service)
    val accessors        = generateCapabilityAccessors(module.name, module.serviceName, capabilites)
    val updatedCompanion = generateUpdatedCompanion(style, module, companion, accessors)

    q"""
       ${trees.module}
       $updatedCompanion
     """
  }

  private def generateCapabilityAccessors(
    moduleType: TypeName,
    serviceName: TermName,
    capabilities: List[Capability]
  ): List[Tree] =
    capabilities.map { capability =>
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
    style: Style,
    module: ModuleSummary,
    companion: CompanionSummary,
    capabilityAccessors: List[Tree]
  ): Tree = {

    val accessor: Tree = style match {
      case Style.TraitOnly => EmptyTree
      case Style.Default   => q"object accessors extends Accessors"
      case Style.AliasOnly => q"object > extends Accessors"
    }

    q"""
      object ${companion.name} {

        ..${companion.body}

       trait Accessors extends Service[${module.name}] {
         ..$capabilityAccessors
       }

       $accessor
     }
    """
  }
}
