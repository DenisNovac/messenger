package app.api.services.db
import app.model.{Conversation, ConversationBody, Cookie, User}
import app.init.PostgresSession
import cats.effect.IO
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.postgres.implicits._
import doobie.util.log.LogHandler
import doobie.util.update.Update
import io.circe.syntax._

class PostgresService(session: PostgresSession) {

  import session.transactor

  /** Get user by immutable ID */
  def getUserById(id: Long): IO[List[User]] =
    sql"""
         |SELECT FROM users WHERE id = $id
         |""".stripMargin.query[User].to[List].transact(transactor)

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[User] = ???

  def getUserConversations: Vector[Conversation] = ???

  //: (User, Vector[Conversation])
  def getUserAndConversations(cookie: Option[String]) =
    for {
      c <- cookie
    } yield sql"""
                 |SELECT FROM sessions WHERE id = $c
                 |""".stripMargin

  /*def putCookie(id: String, body: CookieBody): IO[Int] =
    sql"""
         |INSERT INTO sessions(id, body) VALUES ($id, ${body.asJson})
         |""".stripMargin.update.run.transact(transactor)*/

  def getCookie(id: String): Option[Cookie] = ???

  def updateConversation(id: Long, newConv: ConversationBody): Unit = ???

  def createConversation(newConv: Conversation): Unit = ???

  def removeConversation(id: Long): Unit = ???
}
