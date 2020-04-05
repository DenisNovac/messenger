package app

import app.impl.{AkkaHttpServer, Http4sServer, ServerImpl}
import app.model.ServerConfig
import cats.Monad
import com.typesafe.scalalogging.LazyLogging
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.io.StdIn

object Messenger extends App with LazyLogging {

  logger.info("Messenger is starting...")

  val config: ServerConfig = ConfigSource.file("./application.conf").load[ServerConfig] match {
    case Left(value)  => throw new Error(value.toString)
    case Right(value) => value
  }

  logger.info(s"Application config: $config")

  /** Server startup */
  val server: ServerImpl = config.server match {
    case "akka" =>
      logger.info("Starting Akka Http Server")
      new AkkaHttpServer(config)
    case "http4s" =>
      logger.info("Starting http4s server")
      new Http4sServer(config)
    case _ => throw new IllegalArgumentException("No such server type. Try 'akka' or 'http4s'")
  }

  println("Server is started. Press enter to exit...")
  StdIn.readLine() // let it run until user presses return
  logger.info("Shutting down by user's request...")
  server.stop()

}
