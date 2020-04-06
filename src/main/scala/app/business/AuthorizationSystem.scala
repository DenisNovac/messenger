package app.business

import java.time.Instant
import java.util.UUID

import app.ServerConfigReader
import app.model.ServerConfig
import sttp.model.{CookieValueWithMeta, StatusCode}

import scala.concurrent.duration.FiniteDuration
import cats.syntax.option._
import com.typesafe.scalalogging.LazyLogging

object AuthorizationSystem extends LazyLogging {

  val config: ServerConfig          = ServerConfigReader.config
  val cookieTimeout: FiniteDuration = config.sessionTimeout

  private var cookies: Map[String, (CookieValueWithMeta, Option[Instant])] = Map.empty

  private def getExpiration: Option[Instant] = cookieTimeout match {
    case value if value.toSeconds == 0 => None
    case value =>
      val millis = Instant.now().toEpochMilli + value.toMillis
      Instant.ofEpochMilli(millis).some
  }

  private def isNotExpired(expires: Option[Instant]): Boolean = expires match {
    case Some(value) =>
      if (value.toEpochMilli > Instant.now().toEpochMilli) true
      else false
    case None => true // None is infinite cookie
  }

  def issueCookie: CookieValueWithMeta = {
    val id: String = UUID.randomUUID().toString
    val expires    = getExpiration
    val cookie     = CookieValueWithMeta(value = id, expires = expires, None, None, None, false, false, Map())
    cookies += id -> (cookie, expires)
    cookie
  }

  def isCookieValid(cookie: Option[String]): Boolean = {
    val id = cookie.getOrElse("")

    cookies.get(id) match {
      case Some(value) => isNotExpired(value._2)
      case None        => false
    }

  }

}
