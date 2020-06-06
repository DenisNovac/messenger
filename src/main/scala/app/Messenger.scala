package app

import app.impl.{AkkaHttpServer, Http4sServer, ServerImpl}
import app.init.Init
import cats.syntax.applicative._
import cats.effect.ExitCase._
import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

object Messenger extends IOApp with LazyLogging {

  private val config                        = Init.config
  implicit private val ec: ExecutionContext = Init.ec

  logger.info(s"Application config: $config")

  /** Server startup */
  private val server: ServerImpl = config.server match {
    case "http4s" =>
      logger.info("Starting http4s server")
      new Http4sServer

    case _ => throw new IllegalArgumentException("No such server type. Only 'http4s' has implementation")
  }

  println("Server is started. Enter 'Ctrl + C` to stop server...")

  override def run(args: List[String]): IO[ExitCode] = server.server.guaranteeCase {
    case Canceled =>
      server.stop()
      Init.stop()
      logger.info("Application aborted, resources closed").pure[IO]
    case _ =>
      server.stop()
      Init.stop()
      logger.info("Normal application exit").pure[IO]
  }
}
