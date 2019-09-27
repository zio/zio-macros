import sbt._
import Keys._

import explicitdeps.ExplicitDepsPlugin.autoImport._
import sbtcrossproject.CrossPlugin.autoImport.CrossType

object BuildHelper {
  val testDeps        = Seq("org.scalatest" %% "scalatest" % "3.0.8" % "test")
  val compileOnlyDeps = Nil

  private val stdOptions = Seq(
    "-Xfatal-warnings",
    "-language:higherKinds",
    "-language:existentials",
    "-explaintypes",
    "-Yrangepos",
    "-Xlint:_,-type-parameter-shadow",
    "-Ywarn-numeric-widen"
  )

  private def optimizerOptions(optimize: Boolean) =
    if (optimize)
      Nil
    else Nil

  val replSettings = Seq(
    // In the repl most warnings are useless or worse.
    // This is intentionally := as it's more direct to enumerate the few
    // options we do want than to try to subtract off the ones we don't.
    // One of -Ydelambdafy:inline or -Yrepl-class-based must be given to
    // avoid deadlocking on parallel operations, see
    //   https://issues.scala-lang.org/browse/SI-9076
    scalacOptions in Compile in console := Seq(
      "-Ypartial-unification",
      "-language:higherKinds",
      "-language:existentials",
      "-Yno-adapted-args",
      "-Xsource:2.13",
      "-Yrepl-class-based"
    )
  )

  def extraOptions(scalaVersion: String, optimize: Boolean) =
    CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        Seq(
          "-Ymacro-annotations"
        ) ++ optimizerOptions(optimize)
      case Some((2, 12)) =>
        Seq(
          "-opt-warnings",
          "-Ywarn-extra-implicit",
          // "-Ywarn-unused:_,imports",
          // "-Ywarn-unused:imports",
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xfuture",
          "-Xsource:2.13",
          "-Xmax-classfile-name",
          "242"
        ) ++ optimizerOptions(optimize)
      case Some((2, 11)) =>
        Seq(
          "-Ypartial-unification",
          "-Yno-adapted-args",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit",
          "-Xexperimental",
          "-Ywarn-unused-import",
          "-Xfuture",
          "-Xsource:2.13",
          "-Xmax-classfile-name",
          "242"
        )
      case _ => Seq.empty
    }

  def stdSettings(prjName: String) =
    Seq(
      name := s"$prjName",
      scalacOptions := stdOptions,
      crossScalaVersions := Seq("2.12.8", "2.13.0", "2.11.12"),
      scalaVersion in ThisBuild := crossScalaVersions.value.head,
      scalacOptions := stdOptions ++ extraOptions(scalaVersion.value, optimize = !isSnapshot.value),
      libraryDependencies ++= compileOnlyDeps ++ testDeps ++ {
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
      Compile / unmanagedSourceDirectories ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, 11)) =>
            Seq(
              CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + "-2.11")),
              CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + "-2.11"))
            ).flatten
          case Some((2, x)) if x >= 12 =>
            Seq(
              CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + "-2.12+")),
              CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + "-2.12+"))
            ).flatten
          case _ =>
            Nil
        }
      },
      Test / unmanagedSourceDirectories ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, 11)) =>
            Seq(file(sourceDirectory.value.getPath + "/test/scala-2.11"))
          case Some((2, x)) if x >= 12 =>
            Seq(file(sourceDirectory.value.getPath + "/test/scala-2.12+"))
        }
      }
    ) ++ replSettings

  implicit class ModuleHelper(p: Project) {
    def module: Project = p.in(file(p.id)).settings(stdSettings(p.id))
  }
}
