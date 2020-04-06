package app.model

import java.time.Instant

import app.model.Message._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.CookieValueWithMeta

object DatabaseAbstraction extends LazyLogging {

  case class User(id: Long, name: String, password: String)
  case class Conversation(id: Long, participants: Vector[Long])
  case class CookieBody(user: User, expires: Option[Instant], body: CookieValueWithMeta)

  val users: Map[Long, User] = Map(
    1L -> User(1, "denis", "123"),
    2L -> User(2, "philip", "456"),
    3L -> User(3, "anon", "anon")
  )

  private var messages: Vector[NormalizedTextMessage] = Vector.empty

  def putMessage(msg: NormalizedTextMessage): Unit = {
    logger.info(s"New message: $msg")
    messages :+= msg
  }

  def getMessages: Vector[NormalizedTextMessage] = {
    val v = messages
    v
  }

  private var conversations: Vector[Conversation] =
    Vector(Conversation(1, Vector(1, 2))) // one test conversation

  def startConversation(cnv: Conversation): Unit = {
    logger.info(s"New conversation: $cnv")
    conversations :+= cnv
  }

  def getConversations: Vector[Conversation] = {
    val c = conversations
    c
  }

  /** Issued cookies from authorization */
  private var sessions: Map[String, CookieBody] = Map.empty

  def putCookie(id: String, body: CookieBody): Unit =
    sessions += id -> body

  def getCookie(id: String): Option[CookieBody] =
    sessions.get(id)

  /**
    * Util method which gives user and his conversations from cookie.
    * It is useful since cookie is in every request and there is no usernames after authorization
    * */
  def getUserAndConversations(cookie: Option[String]): (User, Vector[Long]) = {
    val user: User = DatabaseAbstraction.getCookie(cookie.get).get.user
    val userConversations: Vector[Long] =
      DatabaseAbstraction.getConversations.filter(_.participants.contains(user.id)).map(_.id)

    (user, userConversations)
  }

}
