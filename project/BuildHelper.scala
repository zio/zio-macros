import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.CrossType
import sbtbuildinfo._
import BuildInfoKeys._

object BuildHelper {
  val testDeps        = Seq("org.scalatest"   %% "scalatest"    % "3.0.8" % "test")
  val compileOnlyDeps = Seq("com.github.ghik" %% "silencer-lib" % "1.4.2" % "provided")

  val zioVersion = "1.0.0-RC14"

  private val stdOptions = Seq(
    "-deprecation",
    "-encoding",
    "UTF-8",
    "-feature",
    "-unchecked"
  )

  private val std2xOptions = Seq(
    // "-Ymacro-debug-lite",
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-explaintypes",
    "-Yrangepos",
    "-Xsource:2.13",
    "-Xlint:_,-type-parameter-shadow",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  )

  val buildInfoSettings = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, isSnapshot),
    buildInfoPackage := "zio",
    buildInfoObject := "BuildInfoZioMacros"
  )

  val optimizerOptions = {
    Seq(
      "-opt:l:inline",
      "-opt-inline-from:zio.internal.**"
    )
  }

  def extraOptions(scalaVersion: String) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        std2xOptions ++ optimizerOptions
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused:_,imports",
          "-Ywarn-unused:imports",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit"
        ) ++ std2xOptions ++ optimizerOptions
      case Some((2, 11)) =>
        Seq(
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xexperimental",
          "-Ywarn-unused-import"
        ) ++ std2xOptions
      case _ => Seq.empty
    }

  def stdSettings(prjName: String) =
    Seq(
      name := s"$prjName",
      scalacOptions := stdOptions,
      crossScalaVersions := Seq("2.13.0", "2.12.8", "2.11.12"),
      scalaVersion in ThisBuild := crossScalaVersions.value.head,
      scalacOptions := stdOptions ++ extraOptions(scalaVersion.value),
      libraryDependencies ++= compileOnlyDeps ++ testDeps ++ Seq(
        compilerPlugin("org.typelevel"   %% "kind-projector"  % "0.10.3"),
        compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.4.2")
      ) ++ {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, x)) if x >= 13 =>
            Nil
          case _ =>
            Seq(compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full)))
        }
      },
      parallelExecution in Test := true,
      incOptions ~= (_.withLogRecompileOnMacro(true)),
      autoAPIMappings := true,
      unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library"),
      Compile / unmanagedSourceDirectories ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, x)) if x <= 11 =>
            CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + "-2.11")) ++
            CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + "-2.11"))
          case Some((2, x)) if x >= 12 =>
            CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + "-2.12+")) ++
            CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + "-2.12+"))
          case _ => Nil
        }
      },
      Test / unmanagedSourceDirectories ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, x)) if x <= 11 =>
            Seq(file(sourceDirectory.value.getPath + "/test/scala-2.11"))
          case Some((2, x)) if x >= 12 =>
            Seq(
              file(sourceDirectory.value.getPath + "/test/scala-2.12"),
              file(sourceDirectory.value.getPath + "/test/scala-2.12+")
            )
          case _ => Nil
        }
      }
    )

  def macroSettings() =
    Seq(
      scalacOptions --= Seq("-deprecation", "-Xfatal-warnings"),
      libraryDependencies ++= Seq(
        "org.scala-lang" %  "scala-reflect"  % scalaVersion.value % "provided",
        "org.scala-lang" %  "scala-compiler" % scalaVersion.value % "provided"
      )
    )

  def examplesSettings() =
    Seq(
      skip in publish := true,
      libraryDependencies += "dev.zio" %% "zio" % zioVersion
    )

  implicit class ModuleHelper(p: Project) {
    def module: Project = p.in(file(p.id)).settings(stdSettings(p.id))
  }
}
