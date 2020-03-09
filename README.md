# ZIO-MACROS

| CI | Release | Issues |  Discord |
| --- | --- | --- | --- |
| [![Build Status][badge-ci]][link-ci] | [![Release Artifacts][badge-sonatype]][link-sonatype] | [![Average time to resolve an issue][badge-iim]][link-iim] | [![badge-discord]][link-discord] |

Scrap boilerplate in your ZIO projects.
Learn more about ZIO at:

- [Homepage](https://zio.dev)

This project is a loose collection of different macro-based solutions to simplify your code. Head to the specific docs to
learn more about the ones that interest you.

- [accessible](docs/accessible.md) - Generate public accessors for ZIO services.
- [mockable](docs/mockable.md) - Automatically derive mockable implementations for your services.

## Installation

The macros have been split into two subprojects.

- `zio-macros-core` has no dependencies other than `zio`, it contains _accessible_, _delegate_ and _mix_

```scala
"dev.zio" %% "zio-macros-core" % "<version>"
```

- `zio-macros-test` depends on `zio-test` and `zio-macros-core`, it contains _mockable_

```scala
"dev.zio" %% "zio-macros-test" % "<version>"
```

As this project is heavily using macros you will need to enable them:

- If using a scala version < 2.13 you need to add the macro paradise compiler plugin.

```scala
compilerPlugin(("org.scalamacros" % "paradise"  % "2.1.1") cross CrossVersion.full)
```

- If using scala 2.13 you need to add the macro annotation compiler options.
```scala
scalacOptions += "-Ymacro-annotations"
```


[badge-ci]: https://circleci.com/gh/zio/zio-macros/tree/master.svg?style=svg
[badge-sonatype]: https://img.shields.io/nexus/r/https/oss.sonatype.org/dev.zio/zio-macros-core_2.12.svg
[badge-iim]: https://isitmaintained.com/badge/resolution/zio/zio-macros.svg
[badge-discord]: https://img.shields.io/discord/629491597070827530?logo=discord "chat on discord"

[link-ci]: https://circleci.com/gh/zio/zio-macros/tree/master
[link-sonatype]: https://oss.sonatype.org/content/repositories/releases/dev/zio/zio-macros-core_2.12/
[link-iim]: https://isitmaintained.com/project/zio/zio-macros
[link-discord]: https://discord.gg/2ccFBr4 "Discord"
