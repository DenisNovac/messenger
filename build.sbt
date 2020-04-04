name := "messenger"

version := "0.1"

scalaVersion := "2.13.1"

// https://mvnrepository.com/artifact/com.typesafe.akka/akka-stream
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.6.4"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.11"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.11"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.3.0-SNAP2" % Test
