package app.api.services

import java.time.Instant
import java.util.UUID

import app.api.services.db.InMemoryDatabase
import app.init.Init
import app.model.{Authorize, Cookie, ServerConfig}
import com.typesafe.scalalogging.LazyLogging
import sttp.model.CookieValueWithMeta
import cats.syntax.option._
import doobie.implicits._
import doobie.util.Read
import doobie.util.update.Update
import io.circe.Json
import io.circe.syntax._

import scala.concurrent.duration.FiniteDuration

// for putting jsons in SQL-queries
import app.model.PostgresJsonMapping._
import doobie.postgres.implicits._

object AuthService extends LazyLogging {
  private val transactor            = Init.postgres.transactor
  val config: ServerConfig          = Init.config
  val cookieTimeout: FiniteDuration = config.sessionTimeout

  /** Checks user-password pair and issues and cookie if user exist */
  def authorize(authMsg: Authorize): Option[CookieValueWithMeta] =
    InMemoryDatabase.users.get(authMsg.id) match {

      case Some(user) if authMsg.password == user.password =>
        val id: UUID = UUID.randomUUID // id is the value of cookie and database id
        val expires  = getExpiration
        val cookie =
          CookieValueWithMeta(
            value = id.toString,
            expires = expires,
            None,
            None,
            None,
            secure = false,
            httpOnly = false,
            Map()
          )

        val cookieBody = Cookie(id, user.id, expires, cookie)
        InMemoryDatabase.putCookie(id.toString, cookieBody)

        // TODO: only for tests, remove later
        import doobie.implicits.legacy.instant._ // for Instant type
        import app.model.Cookie._                // for Cookie Write

        val sql = "INSERT INTO sessions(id, userid, expires, body) VALUES (?, ?, ?, ?)"
        Update[Cookie](sql).run(Cookie(id, user.id, expires, cookie)).transact(transactor).unsafeRunSync()

        cookie.some

      case _ => None
    }

  /** Cookie must be from the list, must not be expired and must be issued to real user */
  def isCookieValid(cookie: Option[String]): Boolean = {
    val id = cookie.getOrElse("")

    val c = sql"SELECT * FROM sessions WHERE id = ${UUID.fromString(id)}"
      .query[Cookie]
      .unique
      .transact(transactor)
      .unsafeRunSync()

    logger.info(s"${c}")

    /*cki match {
      case x :: Nil =>
        isNotExpired(x.expires) &&                  // cookie is not expired
          InMemoryDatabase.users.contains(x.userid) // cookie belongs to real user
      case _ => false
    }*/
    InMemoryDatabase.getCookie(id) match {
      case Some(value) =>
        isNotExpired(value.expires) &&                  // cookie is not expired
          InMemoryDatabase.users.contains(value.userid) // cookie belongs to real user
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
