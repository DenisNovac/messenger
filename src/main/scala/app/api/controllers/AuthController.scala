package app.api.controllers

import app.api.services.AuthService
import app.model.Authorize
import cats.Monad
import cats.effect.IO
import cats.syntax.either._
import cats.syntax.applicativeError._
import com.typesafe.scalalogging.LazyLogging
import doobie.util.invariant.UnexpectedCursorPosition
import sttp.model.{CookieValueWithMeta, StatusCode}

class AuthController[F[_]: Monad] extends LazyLogging {

  /** Authorization method issues cookie for users */
  def signIn(authMsg: Authorize): IO[Either[StatusCode, CookieValueWithMeta]] = {
    for {
      cookie <- AuthService.authorize(authMsg)
    } yield cookie.asRight[StatusCode]
  }.handleError {
    case e: UnexpectedCursorPosition =>
      logger.error(s"No user ${authMsg.id} found for authorization")
      StatusCode.Forbidden.asLeft[CookieValueWithMeta]
    case e =>
      logger.error(s"Unexpected exception on authentication with id ${authMsg.id}: \n$e")
      StatusCode.ServiceUnavailable.asLeft[CookieValueWithMeta]
  }

  /** Validate cookie */
  def testAuth(cookie: Option[String]): IO[Either[StatusCode, StatusCode]] =
    AuthService.isCookieValid(cookie).map {
      case true  => StatusCode.Ok.asRight[StatusCode]
      case false => StatusCode.Unauthorized.asLeft[StatusCode]
    }
}
