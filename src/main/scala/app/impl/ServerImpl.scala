package app.impl

import app.model.ServerConfig

import com.typesafe.scalalogging.LazyLogging

abstract class ServerImpl extends LazyLogging {
  def stop(): Unit
}
