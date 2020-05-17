package app.api.services

import java.time.Instant
import java.util.UUID

import app.api.services.db.{InMemoryDatabase, PostgresService}
import app.init.Init
import app.model.{Authorize, Cookie, ServerConfig}
import cats.data.OptionT
import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import sttp.model.CookieValueWithMeta
import cats.syntax.option._
import doobie.implicits._
import doobie.util.Read
import doobie.util.update.Update
import io.circe.Json
import io.circe.syntax._
import cats.syntax.applicative._

import scala.concurrent.duration.FiniteDuration

object AuthService extends LazyLogging {
  private val transactor            = Init.postgres.transactor
  val config: ServerConfig          = Init.config
  val cookieTimeout: FiniteDuration = config.sessionTimeout

  /** Checks user-password pair and issues and cookie if user exist */
  def authorize(authMsg: Authorize): IO[CookieValueWithMeta] =
    for {
      user        <- PostgresService.getUserById(authMsg.id)
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
      cookie = Cookie(generatedId, user.id, expires, cookieMeta)
      _      <- PostgresService.putCookie(cookie)

    } yield cookieMeta

  /** Cookie must be from the list, must not be expired and must be issued to real user */
  def isCookieValid(cookie: Option[String]): IO[Boolean] = {
    for {
      maybeCookie  <- OptionT.fromOption[IO](cookie)
      actualCookie <- OptionT.liftF(PostgresService.getCookie(maybeCookie))
      if isNotExpired(actualCookie.expires)
      if InMemoryDatabase.users.contains(actualCookie.userid)
    } yield true
  }.getOrElseF(false.pure[IO])

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
