import sbt._
import sbt.librarymanagement.ModuleID

object dependencies {

  val Fs2: List[ModuleID] = List(
    "co.fs2" %% "fs2-io"     % versions.fs2io,
    "co.fs2" %% "fs2-scodec" % versions.fs2Scodec
  )

  val Typing: List[ModuleID] = List(
    "io.estatico" %% "newtype" % versions.newtype
  )

  val Monix: List[ModuleID] = List(
    "io.monix" %% "monix" % versions.monix
  )

  val Tofu: List[ModuleID] = List(
    "ru.tinkoff" %% "tofu-core"         % versions.tofu,
    "ru.tinkoff" %% "tofu-streams"      % versions.tofu,
    "ru.tinkoff" %% "tofu-fs2-interop"  % versions.tofu,
    "ru.tinkoff" %% "tofu-logging"      % versions.tofu,
    "ru.tinkoff" %% "tofu-env"          % versions.tofu,
    "ru.tinkoff" %% "tofu-derivation"   % versions.tofu,
    "ru.tinkoff" %% "tofu-optics-core"  % versions.tofu,
    "ru.tinkoff" %% "tofu-optics-macro" % versions.tofu
  )

  val Cats: List[ModuleID] = List(
    "org.typelevel" %% "cats-core"     % versions.catsVersion,
    "org.typelevel" %% "cats-effect"   % versions.catsEffectVersion,
    "org.typelevel" %% "cats-mtl-core" % versions.catsMtlVersion,
    "com.olegpy"    %% "meow-mtl-core" % versions.catsMeowMtl,
    "org.typelevel" %% "mouse"         % versions.mouseVersion
  )

  val Manatki: List[ModuleID] = List(
    "org.manatki" %% "derevo"                % versions.manatki,
    "org.manatki" %% "derevo-circe-magnolia" % versions.manatkiCirce,
    "org.manatki" %% "derevo-pureconfig"     % versions.manatkiPureConfig
  )

  val Config: List[ModuleID] = List(
    "com.github.pureconfig" %% "pureconfig"             % versions.pureConfigVersion,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % versions.pureConfigVersion
  )

  val Scopt: List[ModuleID] = List(
    "com.github.scopt" %% "scopt" % versions.scopt
  )

  val Logging: List[ModuleID] = List(
    "ch.qos.logback"     % "logback-classic" % versions.logback,
    "org.slf4j"          % "slf4j-api"       % versions.slf4j,
    "io.chrisdavenport" %% "log4cats-slf4j"  % versions.log4Cats
  )

  val Circe: List[ModuleID] = List(
    "io.circe" %% "circe-fs2" % versions.circeFs2
  )

  val Scodec: List[ModuleID] = List(
    "org.scodec" %% "scodec-core" % versions.scodec,
    "org.scodec" %% "scodec-stream" % versions.scodecStream
  )

  val CompilerPlugins: List[ModuleID] =
    List(
      compilerPlugin(
        "org.typelevel" %% "kind-projector" % versions.KindProjector cross CrossVersion.full
      ),
      compilerPlugin(
        "org.scalamacros" % "paradise" % versions.MacroParadise cross CrossVersion.full
      ),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
    )
}
