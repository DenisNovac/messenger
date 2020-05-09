package app.api.services

import java.time.Instant
import java.util.UUID

import app.api.services.db.InMemoryDatabase
import app.init.Init
import app.model.{Authorize, CookieBody, ServerConfig}
import com.typesafe.scalalogging.LazyLogging
import sttp.model.CookieValueWithMeta
import cats.syntax.option._

import scala.concurrent.duration.FiniteDuration

object AuthService extends LazyLogging {

  val config: ServerConfig          = Init.config
  val cookieTimeout: FiniteDuration = config.sessionTimeout

  /** Checks user-password pair and issues and cookie if user exist */
  def authorize(authMsg: Authorize): Option[CookieValueWithMeta] =
    InMemoryDatabase.users.get(authMsg.id) match {

      case Some(user) if authMsg.password == user.password =>
        val id: String = UUID.randomUUID().toString // id is the value of cookie and database id
        val expires    = getExpiration
        val cookie =
          CookieValueWithMeta(value = id, expires = expires, None, None, None, secure = false, httpOnly = false, Map())

        val cookieBody = CookieBody(user, expires, cookie)
        InMemoryDatabase.putCookie(id, cookieBody)
        cookie.some

      case _ => None
    }

  /** Cookie must be from the list, must not be expired and must be issued to real user */
  def isCookieValid(cookie: Option[String]): Boolean = {
    val id = cookie.getOrElse("")

    InMemoryDatabase.getCookie(id) match {
      case Some(value) =>
        isNotExpired(value.expires) &&                   // cookie is not expired
          InMemoryDatabase.users.contains(value.user.id) // cookie belongs to real user
      case None => false
    }

  }

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
