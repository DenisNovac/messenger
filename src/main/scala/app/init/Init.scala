package app.init

import app.model.ServerConfig
import com.typesafe.scalalogging.LazyLogging
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

/**
  * An object to contain application config for shared access
  */
object Init extends LazyLogging {
  logger.info("Messenger is starting...")
  implicit val ec: ExecutionContext = global

  val config: ServerConfig = ConfigSource.file("./application.conf").load[ServerConfig] match {
    case Left(value)  => throw new Error(value.toString)
    case Right(value) => value
  }

  logger.info("PostgreSQL session is starting...")
  val postgres = new PostgresSession(config.db)

  def stop() =
    postgres.cancelInit()
}
