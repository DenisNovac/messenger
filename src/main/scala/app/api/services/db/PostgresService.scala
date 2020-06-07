package app.api.services.db
import java.util.UUID

import app.model.{AuthorizedSession, Conversation, ConversationBody, MessengerUser}
import cats.effect.IO
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import doobie.implicits._
import app.model.quillmappings.QuillCookieValueWithMetaMapping._
import app.model.quillmappings.QuillInstantMapping._

object PostgresService {

  //implicit val lh: LogHandler = LogHandler.jdkLogHandler

  import app.init.Init.postgres.quillContext._
  import app.init.Init.postgres.transactor

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
    run(
      query[AuthorizedSession].insert(lift(cookie))
    ).transact(transactor) >> IO.unit

  def getCookie(id: String): IO[AuthorizedSession] =
    run(
      query[AuthorizedSession].filter(_.id == lift(UUID.fromString(id))).take(1)
    ).transact(transactor).map(_.head)

  def updateConversation(id: Long, newConv: ConversationBody): Unit = ???

  def createConversation(newConv: Conversation): Unit = ???

  def removeConversation(id: Long): Unit = ???
}
