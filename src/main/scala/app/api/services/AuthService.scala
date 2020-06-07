package app.api.services

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.UUID

import app.api.services.db.{InMemoryDatabase, PostgresService}
import app.init.Init
import app.model.{Authorize, AuthorizedSession, MessengerUser, ServerConfig}
import cats.data.OptionT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import sttp.model.{CookieValueWithMeta, StatusCode}
import cats.syntax.option._
import doobie.implicits._
import io.circe.Json
import io.circe.syntax._
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.flatMap._
import doobie.util.invariant.UnexpectedCursorPosition

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

object AuthService extends LazyLogging {
  private val transactor            = Init.postgres.transactor
  val config: ServerConfig          = Init.config
  val cookieTimeout: FiniteDuration = config.sessionTimeout

  /** Checks user-password pair and issues and cookie if user exist */
  def authorize(authMsg: Authorize): OptionT[IO, CookieValueWithMeta] =
    for {
      user        <- OptionT(PostgresService.checkUserPassword(authMsg.id, authMsg.password))
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
      _      <- OptionT.liftF(PostgresService.putCookie(cookie))
    } yield cookieMeta

  /** Wrapper for actions which needs to be authorized.
    * If token is invalid - it will always return Unauthorized message */
  def authorizedAction[T](
      cookie: Option[String]
  )(action: AuthorizedSession => IO[Either[StatusCode, T]]): IO[Either[StatusCode, T]] = {
    for {
      actualCookie <- OptionT.fromOption[IO](cookie)
      uuid         <- OptionT.fromOption[IO](Try(UUID.fromString(actualCookie)).toOption)
      token        <- OptionT.apply[IO, AuthorizedSession](PostgresService.getCookie(uuid))
    } yield {
      if (isNotExpired(token.expires)) action(token)
      else StatusCode.Unauthorized.asLeft[T].pure[IO]
    }
  }.getOrElse(StatusCode.Unauthorized.asLeft[T].pure[IO]).flatten

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
