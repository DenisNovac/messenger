package app.api.services.db

import app.model._
import com.typesafe.scalalogging.LazyLogging

trait DatabaseService extends LazyLogging {

  /** Get user by immutable ID */
  def getUserById(id: Long): Option[User]

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[User]

  def getUserConversations: Vector[Conversation]

  def getUserAndConversations(cookie: Option[String]): (Long, Vector[Conversation])

  def putCookie(id: String, body: CookieBody): Unit

  def getCookie(id: String): Option[CookieBody]

  def updateConversation(id: Long, newConv: ConversationBody): Unit

  def createConversation(newConv: Conversation): Unit

  def removeConversation(id: Long)

}
