package app.api.services.db

import app.model._
import com.typesafe.scalalogging.LazyLogging

/** In-memory structure for chat */
object InMemoryDatabase extends DatabaseService {

  private var conversationIdCounter = 0

  def getNextConversationId: Long = {
    conversationIdCounter += 1
    conversationIdCounter
  }

  private var conversations: Vector[Conversation] =
    Vector(Conversation(getNextConversationId, ConversationBody("Test Conversation", Vector(1), Vector(1, 2)))) // one test conversation

  def updateConversation(id: Long, newConv: ConversationBody): Unit =
    conversations = conversations.filterNot(_.id == id) :+ Conversation(
      id,
      newConv
    )

  override def getUserConversations: Vector[Conversation] = {
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
  override def getUserAndConversations(cookie: Option[String]): (User, Vector[Conversation]) = {
    val user: User = InMemoryDatabase.getCookie(cookie.get).get.user
    val userConversations: Vector[Conversation] =
      InMemoryDatabase.getUserConversations.filter(_.body.participants.contains(user.id))

    (user, userConversations)
  }

  override def getUserByEmail(id: Long): Option[User]          = ???
  override def createConversation(newConv: Conversation): Unit = ???
  override def removeConversation(id: Long): Unit              = ???
}
