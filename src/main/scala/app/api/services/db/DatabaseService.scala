package app.api.services.db

import app.model._
import com.typesafe.scalalogging.LazyLogging

trait DatabaseService extends LazyLogging {

  /** Get user by immutable ID */
  def getUserById(id: Long): Option[MessengerUser]

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[MessengerUser]

  def getUserConversations: Vector[ConversationApp]

  def getUserAndConversations(cookie: Option[String]): (Long, Vector[ConversationApp])

  def putCookie(id: String, body: AuthorizedSession): Unit

  def getCookie(id: String): Option[AuthorizedSession]

  def updateConversation(id: Long, newConv: ConversationBody): Unit

  def createConversation(newConv: ConversationApp): Unit

  def removeConversation(id: Long)

}
