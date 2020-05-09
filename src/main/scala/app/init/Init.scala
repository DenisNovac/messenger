package app.init

import app.model.ServerConfig
import pureconfig.ConfigSource
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

/**
  * An object to contain application config for shared access
  */
object Init {

  val config: ServerConfig = ConfigSource.file("./application.conf").load[ServerConfig] match {
    case Left(value)  => throw new Error(value.toString)
    case Right(value) => value
  }

  implicit val ec: ExecutionContext = global
  val postgres                      = new PostgresSession(config.db)

}
