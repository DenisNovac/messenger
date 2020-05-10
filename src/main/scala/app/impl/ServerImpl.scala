package app.impl

import cats.effect.{ExitCode, IO}
import com.typesafe.scalalogging.LazyLogging

trait ServerImpl extends LazyLogging {
  def server: IO[ExitCode]
  def stop(): Unit
}
