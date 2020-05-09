package app

import app.impl.{AkkaHttpServer, Http4sServer, ServerImpl}
import app.init.Init
import com.typesafe.scalalogging.LazyLogging

import scala.io.StdIn

object Messenger extends App with LazyLogging {

  logger.info("Messenger is starting...")

  val config = Init.config

  logger.info(s"Application config: $config")

  /** Server startup */
  val server: ServerImpl = config.server match {
    case "akka" =>
      //new AkkaHttpServer
      throw new NotImplementedError("Akka Http implementation is not developed for now")
    case "http4s" =>
      logger.info("Starting http4s server")
      new Http4sServer
    case _ => throw new IllegalArgumentException("No such server type. Only 'http4s' has implementation")
  }

  println("Server is started. Enter 'quit` to stop server...")

  import scala.util.control.Breaks.{break, breakable}

  breakable {
    while (true) {
      StdIn.readLine() match {
        case value if value.trim == "quit" =>
          logger.info("Shutting down by user's request...")
          server.stop()
          break

        case _ =>
          println("Enter 'quit` to stop server...")
      }
    }
  }

}
