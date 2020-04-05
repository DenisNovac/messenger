import sbt.Keys.libraryDependencies

name := "messenger"

version := "0.1"

scalaVersion := "2.13.1"

/** Всё для запуска логгинга так, чтобы работало */
libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.30"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30"
libraryDependencies += "org.log4s" %% "log4s" % "1.8.2"


libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-core" % "0.13.0"
libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "0.13.0"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)


val http4sVersion = "0.21.3"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-core" % http4sVersion,
  "org.http4s" %% "http4s-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion
)

val catsVersion = "2.1.1"

libraryDependencies ++= Seq(
   "org.typelevel" %% "cats-core" % catsVersion,
   "org.typelevel" %% "cats-effect" % catsVersion
)





libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test