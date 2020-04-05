import sbt.Keys.libraryDependencies

name := "messenger"

version := "0.1"

scalaVersion := "2.13.1"

/** Всё для запуска логгинга так, чтобы работало */
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30"
libraryDependencies += "org.slf4j" % "slf4j-api"    % "1.7.30"
libraryDependencies += "org.log4s" %% "log4s"       % "1.8.2"

val tapirVersion = "0.13.0"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core"             % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"    % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-akka-http-server" % tapirVersion
)

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val catsVersion = "2.1.1"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core"   % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion
)

/** Servers */
val http4sVersion = "0.21.3"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-core"         % http4sVersion,
  "org.http4s" %% "http4s-server"       % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion
)

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http"  % "10.1.11"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test
