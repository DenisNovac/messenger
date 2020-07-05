package app.impl

import cats.effect.{ExitCode, IO, Resource}
import com.typesafe.scalalogging.LazyLogging
import org.http4s.server.Server

trait ServerImpl extends LazyLogging {
  def server: Resource[IO, Server[IO]]
  def stop(): Unit
}
