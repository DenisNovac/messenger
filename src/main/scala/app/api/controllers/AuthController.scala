package app.api.controllers

import app.api.services.AuthService
import app.model.{Authorize, AuthorizedSession}
import cats.Monad
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.either._
import cats.syntax.applicative._
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
  }.getOrElse(StatusCode.Unauthorized.asLeft[CookieValueWithMeta])

  /** Validate cookie */
  def testAuth(cookie: Option[String]): IO[Either[StatusCode, AuthorizedSession]] =
    AuthService.authorizedAction(cookie) { token =>
      token.asRight[StatusCode].pure[IO]
    }
}
