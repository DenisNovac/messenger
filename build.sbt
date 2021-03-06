import sbt.Keys.libraryDependencies

name := "messenger"

version := "0.1"

scalaVersion := "2.13.1"

scalacOptions ++= Seq(
  "-language:higherKinds"
)

// https://github.com/softwaremill/tapir/issues/182
assemblyMergeStrategy in assembly := {
  case PathList(ps @ _*) if ps.last.endsWith("pom.properties") => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

libraryDependencies += "ch.qos.logback"             % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.2"

val pureconfigVersion = "0.13.0"

libraryDependencies += "com.github.pureconfig" %% "pureconfig"             % pureconfigVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig-cats"        % pureconfigVersion
libraryDependencies += "com.github.pureconfig" %% "pureconfig-cats-effect" % pureconfigVersion

libraryDependencies += "com.beachape" %% "enumeratum" % "1.6.0"

val tapirVersion = "0.15.3"

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

val doobieVersion = "0.9.0"

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core",
  "org.tpolecat" %% "doobie-postgres",
  "org.tpolecat" %% "doobie-quill"
).map(_ % doobieVersion)

libraryDependencies += "org.liquibase" % "liquibase-core" % "3.9.0"

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies += "org.typelevel" %% "cats-core"   % "2.1.1"
libraryDependencies += "org.typelevel" %% "cats-effect" % "2.1.3"

/** Servers */
val http4sVersion = "0.21.4"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-core",
  "org.http4s" %% "http4s-server",
  "org.http4s" %% "http4s-blaze-server",
  "org.http4s" %% "http4s-dsl"
).map(_ % http4sVersion)

/** Tests dependencies */
libraryDependencies += "org.scalatest"  %% "scalatest" % "3.2.0"       % Test
libraryDependencies += "com.h2database" % "h2"         % "1.4.200"     % Test
libraryDependencies += "org.tpolecat"   %% "doobie-h2" % doobieVersion % Test
