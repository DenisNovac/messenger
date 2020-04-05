package app.business

import sttp.model.StatusCode
import sttp.tapir.server.ServerDefaults.StatusCodes
import cats.Monad
import cats.syntax.either._
import cats.syntax.applicative._ // for pure

import app.model._

/**
  * Logic is separate from routes definitions
  * It may be IO (http4s) or Future (Akka Http)
  */
class RoutesLogic[F[_]: Monad] {

  def health: F[Either[Unit, StatusCode]] =
    StatusCodes.success
      .asRight[Unit]
      .pure[F]

  def hello(name: String): F[Either[Unit, String]] =
    s"Hello, $name".asRight[Unit].pure[F]

  def test: F[Either[Unit, Message]] =
    Message(1, "SERVER", "Test message").asRight[Unit].pure[F]

}
