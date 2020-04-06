package app.impl

import com.typesafe.scalalogging.LazyLogging

trait ServerImpl extends LazyLogging {
  def stop(): Unit
}
