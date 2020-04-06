package app.model

import app.model.DatabaseAbstraction.Conversation

trait Message

object Message {

  case class Authorize(id: Long, password: String)

  case class AddToConversation(conversationId: Long, newUserId: Long)

  case class Sync(timestamp: Long)

  case class SyncConversations()

  /**
    * Messages
    * */
  case class IncomingTextMessage(conversation: Long, timestamp: Long, text: String)

  case class NormalizedTextMessage(conversation: Long, timestamp: Long, text: String, author: Long)

  case class NormTextMessageVector(messages: Vector[NormalizedTextMessage])

  def normalize(msg: IncomingTextMessage, author: Long): NormalizedTextMessage =
    NormalizedTextMessage(msg.conversation, msg.timestamp, msg.text, author)

  case class Conversations(userConversations: Vector[Conversation])
}
