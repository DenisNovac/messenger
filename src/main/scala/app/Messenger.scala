package app

import app.impl.Http4sServer
import app.init.Init
import app.model.ServerConfig
import cats.effect.{ExitCase, ExitCode, IO, IOApp}
import com.typesafe.scalalogging.LazyLogging
import pureconfig.ConfigSource

import scala.concurrent.ExecutionContext

object Messenger extends IOApp with LazyLogging {

  private val config                        = Init.config
  implicit private val ec: ExecutionContext = Init.ec

  logger.info(s"Application config: $config")

  /** Server startup */
  private val server: IO[ExitCode] = config.server match {
    case "http4s" =>
      logger.info("Starting http4s server. Push 'Ctrl + C` to stop server...")
      new Http4sServer().server

    case _ => throw new IllegalArgumentException("No such server type. Only 'http4s' supported")
  }

  override def run(args: List[String]): IO[ExitCode] = server.guaranteeCase {
    case ExitCase.Canceled =>
      IO {
        logger.info("Server is shutting down")
        Init.stop()
      }
  }
}
