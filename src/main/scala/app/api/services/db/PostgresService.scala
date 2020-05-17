package app.api.services.db
import java.util.UUID

import app.model.{Conversation, ConversationBody, Cookie, User}
import app.init.PostgresSession
import cats.effect.IO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.update.Update
import io.circe.syntax._

class PostgresService(session: PostgresSession) {

  import session.transactor

  /** Get user by immutable ID */
  def getUserById(id: Long): IO[User] =
    sql"""
         |SELECT FROM users WHERE id = $id
         |""".stripMargin.query[User].unique.transact(transactor)

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[User] = ???

  def getUserConversations: Vector[Conversation] = ???

  //: (User, Vector[Conversation])
  def getUserAndConversations(cookie: Option[String]) = ???

  def putCookie(cookie: Cookie): IO[Int] =
    Update[Cookie]("INSERT INTO sessions(id, userid, expires, body) VALUES (?, ?, ?, ?)")
      .run(cookie)
      .transact(transactor)

  def getCookie(id: String): IO[Cookie] =
    sql"SELECT * FROM sessions WHERE id = ${UUID.fromString(id)}"
      .query[Cookie]
      .unique
      .transact(transactor)

  def updateConversation(id: Long, newConv: ConversationBody): Unit = ???

  def createConversation(newConv: Conversation): Unit = ???

  def removeConversation(id: Long): Unit = ???
}
