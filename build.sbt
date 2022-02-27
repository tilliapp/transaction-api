import sbt.Keys._

val sharedSettings: Seq[Def.Setting[_]] = Seq(
  organization := "app.tilli",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.13.8"
)

lazy val root = (project in file("."))
  .settings(
    name := "transaction-api",
    libraryDependencies ++= Dependencies.apiDependencies,
    libraryDependencies ++= Dependencies.core,

  )
