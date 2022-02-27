import sbt._

object Dependencies {

  val core = Seq(
    "org.typelevel" %% "cats-core" % "2.7.0",
    "org.typelevel" %% "cats-effect" % "3.3.4",
  )

  val apiDependencies = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.20.0-M5",
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "0.20.0-M5",
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % "0.20.0-M5",
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml" % "0.20.0-M5",
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.20.0-M5",
//    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s" % "0.20.0-M5",
    "org.http4s" %% "http4s-blaze-client" % "0.23.7",
    "org.http4s" %% "http4s-blaze-server" % "0.23.7",
    "org.http4s" %% "http4s-circe" % "0.23.7",


  )
}
