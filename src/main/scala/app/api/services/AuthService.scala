package app.api.services

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import app.api.services.db.{DatabaseService, InMemoryDatabase, TransactorDatabaseService}
import app.model.{Authorize, AuthorizedSession, MessengerUser, ServerConfig}
import cats.data.OptionT
import cats.effect.{Async, IO}
import com.typesafe.scalalogging.LazyLogging
import sttp.model.{CookieValueWithMeta, StatusCode}
import doobie.implicits._
import io.circe.Json
import io.circe.syntax._
import doobie.util.invariant.UnexpectedCursorPosition

import cats.implicits._

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

class AuthService[F[_]: Async](databaseService: DatabaseService[F], cookieTimeout: FiniteDuration) extends LazyLogging {

  /** Checks user-password pair and issues and cookie if user exist */
  def authorize(authMsg: Authorize): OptionT[F, CookieValueWithMeta] =
    for {
      user        <- OptionT(databaseService.checkUserPassword(authMsg.id, authMsg.password))
      generatedId = UUID.randomUUID
      expires     = getExpiration
      cookieMeta = CookieValueWithMeta(
        value = generatedId.toString,
        expires = expires,
        None,
        None,
        None,
        secure = false,
        httpOnly = false,
        Map()
      )
      cookie = AuthorizedSession(generatedId, user.id, expires, cookieMeta)
      _      <- OptionT.liftF(databaseService.putCookie(cookie))
    } yield cookieMeta

  /** Wrapper for actions which needs to be authorized.
    * If token is invalid - it will always return Unauthorized message */
  def authorizedAction[T](
      cookie: Option[String]
  )(action: AuthorizedSession => F[Either[StatusCode, T]]): F[Either[StatusCode, T]] = {
    for {
      actualCookie <- OptionT.fromOption[F](cookie)
      uuid         <- OptionT.fromOption[F](Try(UUID.fromString(actualCookie)).toOption)
      token        <- OptionT.apply[F, AuthorizedSession](databaseService.getCookie(uuid))
    } yield {
      if (isNotExpired(token.expires)) action(token)
      else StatusCode.Unauthorized.asLeft[T].pure[F]
    }
  }.getOrElse(StatusCode.Unauthorized.asLeft[T].pure[F]).flatten

  /** Sums current time and timeout from config */
  private def getExpiration: Option[Instant] = cookieTimeout match {
    case value if value.toSeconds == 0 => None
    case value =>
      val millis = Instant.now().toEpochMilli + value.toMillis
      Instant.ofEpochMilli(millis).some
  }

  /** Checks if current time is lesser than cookie timeout */
  private def isNotExpired(expires: Option[Instant]): Boolean = expires match {
    case Some(value) =>
      if (value.toEpochMilli > Instant.now().toEpochMilli) true
      else false
    case None => true // None is infinite cookie
  }

}
