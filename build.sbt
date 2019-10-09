import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import BuildHelper._

inThisBuild(
  List(
    name := "zio-macros",
    organization := "dev.zio",
    homepage := Some(url("https://github.com/zio/zio-macros")),
    developers := List(
      Developer(
        "mschuwalow",
        "Maxim Schuwalow",
        "maxim.schuwalow@gmail.com",
        url("https://github.com/mschuwalow")
      ),
      Developer(
        "ioleo",
        "Piotr Gołębiewski",
        "ioleo+zio@protonmail.com",
        url("https://github.com/ioleo")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        homepage.value.get,
        "scm:git:git@github.com:zio/zio-macros.git"
      )
    ),
    licenses := Seq("Apache-2.0" -> url(s"${scmInfo.value.map(_.browseUrl).get}/blob/v${version.value}/LICENSE")),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc")
  )
)

ThisBuild / publishTo := sonatypePublishToBundle.value

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("testJVM", ";accessExamplesJVM/test;mockExamplesJVM/test;delegateTestsJVM/test")
addCommandAlias("testJS", ";accessExamplesJS/test;mockExamplesJS/test;delegateTestsJS/test")
addCommandAlias("testRelease", ";set every isSnapshot := false;+clean;+compile")

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )
  .aggregate(
    access.jvm,
    access.js,
    accessExamples.jvm,
    accessExamples.js,
    core.jvm,
    core.js,
    mock.jvm,
    mock.js,
    mockExamples.jvm,
    mockExamples.js,
    delegate.jvm,
    delegate.js,
    delegateTests.jvm,
    delegateTests.js,
    delegateExamples.jvm,
    delegateExamples.js
  )
  .enablePlugins(ScalaJSPlugin)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(stdSettings("zio-macros-core"))
  .settings(macroSettings())

lazy val access = crossProject(JSPlatform, JVMPlatform)
  .in(file("access"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-access"))
  .settings(macroSettings())

lazy val accessExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("access-examples"))
  .dependsOn(access)
  .settings(stdSettings("zio-macros-access-examples"))
  .settings(examplesSettings())

lazy val mock = crossProject(JSPlatform, JVMPlatform)
  .in(file("mock"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-mock"))
  .settings(macroSettings())
  .settings(libraryDependencies += "dev.zio" %% "zio-test" % zioVersion)

lazy val mockExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("mock-examples"))
  .dependsOn(mock)
  .settings(stdSettings("zio-macros-mock-examples"))
  .settings(examplesSettings())

lazy val delegate = crossProject(JSPlatform, JVMPlatform)
  .in(file("delegate"))
  .settings(stdSettings("zio-macros-delegate"))
  .settings(macroSettings())
  .settings(
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, x)) if x <= 11 => Seq("-Ywarn-unused:false")
        case _                       => Seq("-Ywarn-unused:-patvars,-explicits,_")
      }
    }
  )

lazy val delegateTests = crossProject(JSPlatform, JVMPlatform)
  .in(file("delegate-tests"))
  .dependsOn(delegate)
  .settings(stdSettings("zio-macros-delegate-tests"))
  .settings(
    skip in publish := true,
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, x)) if x <= 11 => Seq("-Ywarn-unused:false")
        case _                       => Seq("-Ywarn-unused:-explicits,_")
      }
    }
  )

lazy val delegateExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("delegate-examples"))
  .dependsOn(delegate)
  .settings(stdSettings("zio-macros-delegate-examples"))
  .settings(examplesSettings())
