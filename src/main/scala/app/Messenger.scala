package app

import app.impl.{AkkaHttpServer, Http4sServer, ServerImpl}
import com.typesafe.scalalogging.LazyLogging

import scala.io.StdIn

object Messenger extends App with LazyLogging {

  logger.info("Messenger is starting...")

  val config = ServerConfigReader.config

  logger.info(s"Application config: $config")

  /** Server startup */
  val server: ServerImpl = config.server match {
    case "akka" =>
      logger.info("Starting Akka Http Server")
      new AkkaHttpServer
    case "http4s" =>
      logger.info("Starting http4s server")
      new Http4sServer
    case _ => throw new IllegalArgumentException("No such server type. Try 'akka' or 'http4s'")
  }

  println("Server is started. Press enter to exit...")
  StdIn.readLine() // let it run until user presses return
  logger.info("Shutting down by user's request...")
  server.stop()

}
