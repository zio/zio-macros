# ZIO-MACROS

[![CircleCI][badge-ci]][link-ci]
[![Sonatype][badge-sonatype]][link-Sonatype]
[![Issue Resolution][badge-iim]][link-iim]
[![Gitter][badge-gitter]][link-gitter]

Scrap boilerplate in your ZIO projects.
Learn more about ZIO at:

- [Homepage](https://zio.dev)

This project is a loose collection of different macro-based solutions to simplify your code. Head to the specific docs to
learn more about the ones that interest you.

- [Accessible](docs/accessible.md) - Generate public accessors for ZIO services.
- [Mockable](docs/mockable.md) - Automatically derive mockable implementations for your services.
- [Delegate](docs/delegate.md) - Generate proxies for arbitrary traits / classes.
- [Mix](docs/mix.md) - Derive rules to combine instances of traits into a compound type.

## Installation
All subprojects ship at their own coordinates. Add the relevant dependencies to your build.
```scala
"dev.zio" %% "zio-macros-<subproject>" % "<version>"
```

As this project is heavily using macros you will need to enable them:

* If using a scala version < 2.13 you need to add the macro paradise compiler plugin.
    ```scala
    compilerPlugin(("org.scalamacros" % "paradise"  % "2.1.1") cross CrossVersion.full)
    ```

* If using scala 2.13 you need to add the macro annotation compiler option.
    ```scala
    .settings(
      scalacOptions ++= Seq("-Ymacro-annotations"),
    )
    ```


[badge-ci]: https://circleci.com/gh/zio/zio-macros/tree/master.svg?style=svg
[badge-sonatype]: https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-macros-access_2.12.svg
[badge-iim]: https://isitmaintained.com/badge/resolution/zio/zio-macros.svg
[badge-gitter]: https://badges.gitter.im/ZIO/zio-macros.svg

[link-ci]: https://circleci.com/gh/zio/zio-macros/tree/master
[Link-Sonatype]: https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-macros-access_2.12/
[Link-IIM]: https://isitmaintained.com/project/zio/zio-macros
[link-gitter]: https://gitter.im/ZIO/zio-macros?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
