package app.generic

import sttp.model.StatusCode
import sttp.tapir.server.ServerDefaults.StatusCodes
import cats.Monad                // for generic logic on pure syntax
import cats.syntax.either._      // for asRight
import cats.syntax.applicative._ // for pure

/**
  * Logic is separate from routes definitions
  * It may be IO (http4s) or Future (Akka Http)
  */
class Logic[F[_]: Monad] {

  def health: F[Either[Unit, StatusCode]] =
    StatusCodes.success
      .asRight[Unit]
      .pure[F]

  def hello(name: String): F[Either[Unit, String]] =
    s"Hello, $name".asRight[Unit].pure[F]
}
