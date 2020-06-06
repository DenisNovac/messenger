package app.api.services.db

import app.model._
import com.typesafe.scalalogging.LazyLogging

/** In-memory structure for chat */
object InMemoryDatabase {

  private var conversationIdCounter = 0

  def getNextConversationId: Long = {
    conversationIdCounter += 1
    conversationIdCounter
  }

  private var conversations: Vector[Conversation] =
    Vector(Conversation(getNextConversationId, ConversationBody("Test Conversation", Set(1), Set(), Set(1, 2)))) // one test conversation

  def updateConversation(id: Long, newConv: ConversationBody): Unit =
    conversations = conversations.filterNot(_.id == id) :+ Conversation(
      id,
      newConv
    )

  def getUserConversations: Vector[Conversation] = {
    val c = conversations
    c
  }

  val users: Map[Long, MessengerUser] = Map(
    1L -> MessengerUser(1, "denis", "123"),
    2L -> MessengerUser(2, "philip", "456"),
    3L -> MessengerUser(3, "anon", "anon")
  )

  def getUserById(id: Long): Option[MessengerUser] =
    users.get(id)

  private var messages: Vector[NormalizedTextMessage] = Vector.empty

  def putMessage(msg: NormalizedTextMessage): Unit =
    messages :+= msg

  def getMessages: Vector[NormalizedTextMessage] = {
    val v = messages
    v
  }

  /** Issued cookies from authorization */
  /* private var sessions: Map[String, Cookie] = Map.empty

  def putCookie(id: String, body: Cookie): Unit =
    sessions += id -> body

  def getCookie(id: String): Option[Cookie] =
    sessions.get(id)*/

  /**
    * Util method which gives user and his conversations from cookie.
    * It is useful since cookie is in every request and there is no usernames after authorization
    * */
  def getUserAndConversations(cookie: Option[String]): (Long, Vector[Conversation]) = {
    val userid: Long = PostgresService.getCookie(cookie.get).unsafeRunSync.userid
    val userConversations: Vector[Conversation] =
      InMemoryDatabase.getUserConversations.filter(_.body.participants.contains(userid))

    (userid, userConversations)
  }

  /*override def getUserByEmail(id: Long): Option[User]          = ???
  override def createConversation(newConv: Conversation): Unit = ???
  override def removeConversation(id: Long): Unit              = ???*/
}
