package app.api.services.db
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

import app.model.{AuthorizedSession, Conversation, ConversationBody, MessengerUser}
import app.init.PostgresSession
import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.log.LogHandler
import doobie.util.update.Update
import cats.syntax.applicativeError._
import io.circe.syntax._
import cats.syntax.flatMap._
import io.circe.Json
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.model.CookieValueWithMeta

object PostgresService {

  //implicit val lh: LogHandler = LogHandler.jdkLogHandler

  import app.init.Init.postgres.transactor
  import app.init.Init.postgres.quillContext._

  import app.model.AuthorizedSession._

  implicit val instantEncoding: MappedEncoding[Instant, Date] = MappedEncoding[Instant, Date](Date.from)
  implicit val instantDecoding: MappedEncoding[Date, Instant] = MappedEncoding[Date, Instant](_.toInstant())

  implicit val cookieEncoding: MappedEncoding[CookieValueWithMeta, String] =
    MappedEncoding[CookieValueWithMeta, String] { cookie =>
      cookie.asJson.toString
    }

  implicit val cookieDecoding: MappedEncoding[String, CookieValueWithMeta] =
    MappedEncoding[String, CookieValueWithMeta] { cookie =>
      cookie.asJson.as[CookieValueWithMeta] match {
        case Right(meta) => meta
      }
    }

  /** Get user by immutable ID */
  def getUserById(id: Long): IO[Option[MessengerUser]] =
    run(query[MessengerUser].filter(_.id == lift(id)).take(1)).transact(transactor).map(_.headOption)

  def checkUserPassword(id: Long, pwd: String): IO[Option[MessengerUser]] =
    run {
      query[MessengerUser]
        .filter(_.id == lift(id))
        .filter(_.password == lift(pwd))
        .take(1)
    }.transact(transactor)
      .map(_.headOption)

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[MessengerUser] = ???

  def getUserConversations: Vector[Conversation] = ???

  //: (User, Vector[Conversation])
  def getUserAndConversations(cookie: Option[String]) = ???

  def putCookie(cookie: AuthorizedSession): IO[Unit] =
    (run(
      query[AuthorizedSession].insert(lift(cookie))
    ).transact(transactor) >> IO.unit).handleError {
      case e =>
        println(":DDD")
        throw e
    }

  def getCookie(id: String): IO[AuthorizedSession] =
    run(
      query[AuthorizedSession].filter(_.id == lift(UUID.fromString(id))).take(1)
    ).transact(transactor).map(_.head)

  def updateConversation(id: Long, newConv: ConversationBody): Unit = ???

  def createConversation(newConv: Conversation): Unit = ???

  def removeConversation(id: Long): Unit = ???
}
