package app.api.services.db
import java.util.UUID

import app.model.{Conversation, ConversationBody, Cookie, MessengerUser}
import app.init.PostgresSession
import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.log.LogHandler
import doobie.util.update.Update
import io.circe.syntax._

object PostgresService {

  //implicit val lh: LogHandler = LogHandler.jdkLogHandler

  import app.init.Init.postgres.transactor

  /** Get user by immutable ID */
  def getUserById(id: Long): IO[MessengerUser] =
    sql"SELECT * FROM messenger_user WHERE id = $id".query[MessengerUser].unique.transact(transactor)

  def checkUserPassword(id: Long, pwd: String): IO[MessengerUser] =
    sql"SELECT * FROM messenger_user WHERE id = $id AND password = $pwd".query[MessengerUser].unique.transact(transactor)

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[MessengerUser] = ???

  def getUserConversations: Vector[Conversation] = ???

  //: (User, Vector[Conversation])
  def getUserAndConversations(cookie: Option[String]) = ???

  def putCookie(cookie: Cookie): IO[Int] =
    Update[Cookie]("INSERT INTO session(id, user_id, expires, body) VALUES (?, ?, ?, ?)")
      .run(cookie)
      .transact(transactor)

  def getCookie(id: String): IO[Cookie] =
    sql"SELECT * FROM session WHERE id = ${UUID.fromString(id)}"
      .query[Cookie]
      .unique
      .transact(transactor)

  def updateConversation(id: Long, newConv: ConversationBody): Unit = ???

  def createConversation(newConv: Conversation): Unit = ???

  def removeConversation(id: Long): Unit = ???
}
