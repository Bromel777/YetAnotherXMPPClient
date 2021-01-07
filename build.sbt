import dependencies._

lazy val commonSettings = Seq(
  name := "YetAnotherXMPPClient",
  version := "0.1",
  scalaVersion := "2.12.12",
  organization := "com.github.bromel777",
  scalacOptions ++= commonScalacOptions,
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

val yaXMPPc =
  (project in file("."))
    .settings(commonSettings: _*)
    .settings(
      libraryDependencies ++=
        Fs2 ++
        Typing ++
        Monix ++
        Tofu ++
        Cats ++
        Manatki ++
        CompilerPlugins
    )

lazy val commonScalacOptions = List(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-feature",
  "-unchecked",
  "-Xfuture",
  "-Yno-adapted-args",
  "-Ywarn-numeric-widen",
  "-Ypartial-unification"
)
