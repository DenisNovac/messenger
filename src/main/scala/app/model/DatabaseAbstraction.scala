package app.model

import java.time.Instant

import app.model.Message._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.CookieValueWithMeta

/** In-memory structure for chat */
object DatabaseAbstraction extends LazyLogging {

  case class User(id: Long, name: String, password: String) {
    def prettyName: String = s"$name#$id" // name like Sam#111
  }
  case class Conversation(id: Long, name: String, admins: Vector[Long], participants: Vector[Long])

  case class CookieBody(user: User, expires: Option[Instant], body: CookieValueWithMeta)

  private var conversationIdCounter = 0

  def getNextConversationId: Long = {
    conversationIdCounter += 1
    conversationIdCounter
  }

  private var conversations: Vector[Conversation] =
    Vector(Conversation(getNextConversationId, "Test Conversation", Vector(1), Vector(1, 2))) // one test conversation

  def updateConversations(id: Long, newConv: Conversation): Unit =
    conversations = conversations.filterNot(_.id == id) :+ Conversation(
      id,
      newConv.name,
      newConv.admins,
      newConv.participants
    )

  def getConversations: Vector[Conversation] = {
    val c = conversations
    c
  }

  val users: Map[Long, User] = Map(
    1L -> User(1, "denis", "123"),
    2L -> User(2, "philip", "456"),
    3L -> User(3, "anon", "anon")
  )

  def getUserById(id: Long): Option[User] =
    users.get(id)

  private var messages: Vector[NormalizedTextMessage] = Vector.empty

  def putMessage(msg: NormalizedTextMessage): Unit =
    messages :+= msg

  def getMessages: Vector[NormalizedTextMessage] = {
    val v = messages
    v
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
  def getUserAndConversations(cookie: Option[String]): (User, Vector[Conversation]) = {
    val user: User = DatabaseAbstraction.getCookie(cookie.get).get.user
    val userConversations: Vector[Conversation] =
      DatabaseAbstraction.getConversations.filter(_.participants.contains(user.id))

    (user, userConversations)
  }

}
