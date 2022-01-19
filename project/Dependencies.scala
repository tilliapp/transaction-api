import sbt._

object Dependencies {

  val core = Seq(
    "org.typelevel" %% "cats-core" % "2.7.0",
    "org.typelevel" %% "cats-effect" % "3.3.4",
  )

  val apiDependencies = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.20.0-M5",
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.20.0-M5"

  )
}
