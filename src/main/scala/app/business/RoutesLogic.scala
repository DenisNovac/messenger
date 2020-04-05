package app.business

import cats.Monad
import cats.syntax.either._
import cats.syntax.applicative._ // for pure

import app.model._

/**
  * Logic is separate from routes definitions
  * It may be IO (http4s) or Future (Akka Http)
  */
class RoutesLogic[F[_]: Monad] {

  def health: F[Either[Unit, Message]] =
    Message(1, "SERVER", "OK")
      .asRight[Unit]
      .pure[F]

}
