lazy val commonSettings = Seq (
  name := "YetAnotherXMPPClient",
  version := "0.1",
  scalaVersion := "2.13.4",
  organization := "com.github.bromel777",
  resolvers += Resolver.sonatypeRepo("public"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

val yaXMPPc = (project in file(".")).settings(commonSettings: _*)