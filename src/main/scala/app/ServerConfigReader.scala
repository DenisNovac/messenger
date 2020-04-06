package app

import app.model.ServerConfig
import pureconfig.ConfigSource
import pureconfig.ConfigSource
import pureconfig.generic.auto._

/**
  * An object to contain application config for shared access
  */
object ServerConfigReader {

  val config: ServerConfig = ConfigSource.file("./application.conf").load[ServerConfig] match {
    case Left(value)  => throw new Error(value.toString)
    case Right(value) => value
  }
}
