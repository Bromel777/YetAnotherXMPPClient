import sbt.librarymanagement.ModuleID
import sbt.{CrossVersion, compilerPlugin, _}

object dependencies {

  val Fs2: List[ModuleID] = List(
    "co.fs2" %% "fs2-core" % versions.fs2,
    "co.fs2" %% "fs2-io" % versions.fs2
  )

  val Typing: List[ModuleID] = List(
    "io.estatico" %% "newtype"        % versions.newtype
  )
}
