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
addCommandAlias("testJVM", ";coreTestsJVM/test;testTestsJVM/test")
addCommandAlias("testJS", ";coreTestsJS/test;testTestsJS/test")
addCommandAlias("testRelease", ";set every isSnapshot := false;+clean;+compile")

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )
  .aggregate(
    core.jvm,
    core.js,
    coreExamples.jvm,
    coreExamples.js,
    coreTests.jvm,
    coreTests.js,
    test.jvm,
    test.js,
    testExamples.jvm,
    testExamples.js,
    testTests.jvm,
    testTests.js
  )
  .enablePlugins(ScalaJSPlugin)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(stdSettings("zio-macros-core"))
  .settings(macroSettings)
  .settings(libraryDependencies += "dev.zio" %% "zio" % zioVersion)

lazy val coreExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("core-examples"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-core-examples"))
  .settings(examplesSettings)

lazy val coreTests = crossProject(JSPlatform, JVMPlatform)
  .in(file("core-tests"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-core-tests"))
  .settings(testSettings)

lazy val test = crossProject(JSPlatform, JVMPlatform)
  .in(file("test"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-test"))
  .settings(macroSettings)
  .settings(libraryDependencies += "dev.zio" %% "zio-test" % zioVersion)

lazy val testExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("test-examples"))
  .dependsOn(test)
  .settings(stdSettings("zio-macros-test-examples"))
  .settings(examplesSettings)

lazy val testTests = crossProject(JSPlatform, JVMPlatform)
  .in(file("test-tests"))
  .dependsOn(test)
  .settings(stdSettings("zio-macros-test-tests"))
  .settings(testSettings)
