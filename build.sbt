import sbt.Keys.libraryDependencies

name := "messenger"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-language:higherKinds"
)

libraryDependencies += "ch.qos.logback"             % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"
libraryDependencies += "com.github.pureconfig"      %% "pureconfig"     % "0.12.3"
libraryDependencies += "com.beachape"               %% "enumeratum"     % "1.6.0"

val tapirVersion = "0.15.0"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core",
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server",
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-model",
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe",
  "com.softwaremill.sttp.tapir" %% "tapir-openapi-circe-yaml",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-http4s",
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-akka-http"
).map(_ % tapirVersion)

val doobieVersion = "0.8.8"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-postgres"
).map(_ % doobieVersion)

libraryDependencies += "org.liquibase" % "liquibase-core" % "3.9.0"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val catsVersion = "2.1.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core",
  "org.typelevel" %% "cats-effect"
).map(_ % catsVersion)

/** Servers */
val http4sVersion = "0.21.3"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-core",
  "org.http4s" %% "http4s-server",
  "org.http4s" %% "http4s-blaze-server",
  "org.http4s" %% "http4s-dsl"
).map(_ % http4sVersion)

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http"  % "10.1.11"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test

// https://github.com/softwaremill/tapir/issues/182
assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last.endsWith("pom.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
