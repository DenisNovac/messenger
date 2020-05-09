package app.api.controllers

import app.api.services.AuthService
import app.model.Authorize
import cats.Monad
import cats.syntax.either._
import cats.syntax.applicative._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.{CookieValueWithMeta, StatusCode}

class AuthController[F[_]: Monad] extends LazyLogging {

  /** Authorization method issues cookie for users */
  def signIn(authMsg: Authorize): F[Either[StatusCode, CookieValueWithMeta]] =
    AuthService.authorize(authMsg) match {
      case Some(value) =>
        logger.info(s"User ${authMsg.id} successfully authorized")
        value.asRight[StatusCode].pure[F]
      case None =>
        logger.warn(s"User ${authMsg.id} couldn't authorize")
        StatusCode.Forbidden.asLeft[CookieValueWithMeta].pure[F]
    }

  /** Validate cookie */
  def testAuth(cookie: Option[String]): F[Either[StatusCode, StatusCode]] =
    if (AuthService.isCookieValid(cookie)) {
      StatusCode.Ok.asRight[StatusCode].pure[F]
    } else {
      StatusCode.Unauthorized.asLeft[StatusCode].pure[F]
    }
}
