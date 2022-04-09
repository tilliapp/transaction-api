import sbt._

object Dependencies {

  val core = Seq(
    "org.typelevel" %% "cats-core" % "2.7.0",
    "org.typelevel" %% "cats-free" % "2.7.0",
    "org.typelevel" %% "cats-effect" % "3.3.5",

    "ch.qos.logback" % "logback-classic" % "1.2.11",
  )

  val testDependencies = Seq(
    "org.scalatest" %% "scalatest" % "3.2.11",
  )

  val apiDependencies = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.20.0-M10",
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.19.0-M4",
    "org.http4s" %% "http4s-blaze-client" % "0.23.10",
    "org.http4s" %% "http4s-blaze-server" % "0.23.10",
    "org.http4s" %% "http4s-circe" % "0.23.10",
  )

  val serdesDependencies = Seq(
    "io.circe" %% "circe-optics" % "0.14.1"
  )

  val web3Dependencies = Seq(
    "org.web3j" % "core" % "4.9.1"
  )
}
