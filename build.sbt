import sbt.Keys._

val sharedSettings: Seq[Def.Setting[_]] = Seq(
  organization := "app.tilli",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.13.8",
  scalacOptions ++= Seq(
    //    "-Ypartial-unification",
    "-deprecation",
    "-unchecked",
    "-feature",
    "-language:implicitConversions",
    "-language:higherKinds",
    "-Ymacro-annotations",
    "-Ywarn-dead-code",
    "-Xlint:unused",
    "-Wdead-code",
  ),
  fork := true,
  publishArtifact in Test := true,
  test in assembly := {},
  assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") => MergeStrategy.singleOrError
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
)

lazy val root = (project in file("."))
  .settings(
    name := "transaction-api",
    sharedSettings,
    libraryDependencies ++= Dependencies.core,
    libraryDependencies ++= Dependencies.utils,
    libraryDependencies ++= Dependencies.testDependencies,
    libraryDependencies ++= Dependencies.apiDependencies,
    libraryDependencies ++= Dependencies.serdesDependencies,
    libraryDependencies ++= Dependencies.web3Dependencies,
    libraryDependencies ++= Dependencies.dataDependencies,
    mainClass in assembly := Some("app.tilli.app.ApiApp"),
    assemblyJarName in assembly := "run.jar"
  )
