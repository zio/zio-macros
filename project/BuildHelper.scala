import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.CrossType
import sbtbuildinfo._
import BuildInfoKeys._

object BuildHelper {
  val zioVersion = "1.0.0-RC14"

  private val testDeps = Seq(
    "org.scalatest"   %% "scalatest"    % "3.0.8" % "test"
  )

  private def compileOnlyDeps(scalaVersion: String) = {
    val stdCompileOnlyDeps = Seq(
      "com.github.ghik" %% "silencer-lib" % "1.4.2" % "provided",
      compilerPlugin("org.typelevel"   %% "kind-projector"  % "0.10.3"),
      compilerPlugin("com.github.ghik" %% "silencer-plugin" % "1.4.2")
    )
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, x)) if x <= 12 =>
        stdCompileOnlyDeps ++ Seq(
          compilerPlugin(("org.scalamacros" % "paradise" % "2.1.1").cross(CrossVersion.full))
        )
      case _ => stdCompileOnlyDeps
    }
  }

  private def compilerOptions(scalaVersion: String, optimize: Boolean) = {
    val stdOptions = Seq(
      //"-Ymacro-debug-lite",
      //"-Ymacro-debug-verbose",
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-unchecked",
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

    def optimizerOptions(optimize: Boolean) =
      if (optimize)
        Seq(
          "-opt:l:inline"
        )
      else Nil

    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        Seq(
          "-Ymacro-annotations"
        ) ++ stdOptions ++ optimizerOptions(optimize)
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          "-Ywarn-unused",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit"
        ) ++ stdOptions ++ optimizerOptions(optimize)
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
        ) ++ stdOptions
      case _ => Seq.empty
    }
  }

  val buildInfoSettings = Seq(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, isSnapshot),
    buildInfoPackage := "zio",
    buildInfoObject := "BuildInfoZioMacros"
  )

  def stdSettings(prjName: String) =
    Seq(
      name := s"$prjName",
      crossScalaVersions := Seq("2.13.0", "2.12.8", "2.11.12"),
      scalaVersion in ThisBuild := crossScalaVersions.value.head,
      scalacOptions := compilerOptions(scalaVersion.value, optimize = !isSnapshot.value),
      libraryDependencies ++= compileOnlyDeps(scalaVersion.value) ++ testDeps,
      parallelExecution in Test := true,
      incOptions ~= (_.withLogRecompileOnMacro(true)),
      autoAPIMappings := true,
      unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
    )

  def macroSettings() =
    Seq(
      scalacOptions --= Seq("-deprecation", "-Xfatal-warnings"),
      libraryDependencies ++= Seq(
        "org.scala-lang" % "scala-reflect"  % scalaVersion.value % "provided",
        "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"
      )
    )

  def examplesSettings() =
    Seq(
      skip in publish := true,
      libraryDependencies += "dev.zio" %% "zio" % zioVersion
    )
}
