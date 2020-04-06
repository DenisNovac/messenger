package app.business

import java.time.Instant
import java.util.UUID

import app.ServerConfigReader
import app.model.{Authorize, ServerConfig}
import sttp.model.{CookieValueWithMeta, StatusCode}

import scala.concurrent.duration.FiniteDuration
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging

object AuthorizationSystem extends LazyLogging {

  val config: ServerConfig          = ServerConfigReader.config
  val cookieTimeout: FiniteDuration = config.sessionTimeout

  /** Issued cookies */
  private var sessions: Map[String, (CookieValueWithMeta, Option[Instant])] = Map.empty

  private val users: Map[String, String] = Map(
    "denis" -> "123",
    "filya" -> "456"
  )

  /** Checks user-password pair and issues and cookie if user exist */
  def authorize(authMsg: Authorize): Option[CookieValueWithMeta] =
    users.get(authMsg.id) match {

      case Some(pwd) if authMsg.password == pwd =>
        val id: String = UUID.randomUUID().toString
        val expires    = getExpiration
        val cookie     = CookieValueWithMeta(value = id, expires = expires, None, None, None, false, false, Map())
        sessions += id -> (cookie, expires)
        cookie.some

      case _ => None
    }

  /** Cookie must be from the list and must not me expired */
  def isCookieValid(cookie: Option[String]): Boolean = {
    val id = cookie.getOrElse("")
    sessions.get(id) match {
      case Some(value) => isNotExpired(value._2)
      case None        => false
    }
  }

  /** Sums current time and timeout from config */
  private def getExpiration: Option[Instant] = cookieTimeout match {
    case value if value.toSeconds == 0 => None
    case value =>
      val millis = Instant.now().toEpochMilli + value.toMillis
      Instant.ofEpochMilli(millis).some
    case _ => None
  }

  /** Checks if current time is lesser than cookie timeout */
  private def isNotExpired(expires: Option[Instant]): Boolean = expires match {
    case Some(value) =>
      if (value.toEpochMilli > Instant.now().toEpochMilli) true
      else false
    case None => true // None is infinite cookie
  }

}
