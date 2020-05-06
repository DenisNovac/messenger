package app.services

import cats.Monad
import cats.syntax.either._
import cats.syntax.applicative._
import sttp.model.StatusCode

class UtilService[F[_]: Monad] {

  /** Returns 200 */
  def health: F[Either[Unit, StatusCode]] =
    StatusCode.Ok
      .asRight[Unit]
      .pure[F]

}
