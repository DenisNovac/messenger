package app.api.controllers

import app.api.services.AuthService
import app.model.Authorize
import cats.Monad
import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.syntax.either._
import cats.syntax.applicative._
import com.typesafe.scalalogging.LazyLogging
import sttp.model
import sttp.model.{CookieValueWithMeta, StatusCode}

class AuthController[F[_]: Monad] extends LazyLogging {

  /** Authorization method issues cookie for users */
  def signIn(authMsg: Authorize): IO[Either[StatusCode, CookieValueWithMeta]] = {
    for {
      cookie <- AuthService.authorize(authMsg).attempt
    } yield cookie
  }.map {
    case Left(value) =>
      logger.error(s"Expected error on signIn with id ${authMsg.id}: ${value.getMessage}")
      StatusCode.Forbidden.asLeft[CookieValueWithMeta]
    case Right(value) => value.asRight[StatusCode]
  }

  /** Validate cookie */
  def testAuth(cookie: Option[String]): IO[Either[StatusCode, StatusCode]] =
    AuthService.isCookieValid(cookie).map {
      case true  => StatusCode.Ok.asRight[StatusCode]
      case false => StatusCode.Unauthorized.asLeft[StatusCode]
    }
}
