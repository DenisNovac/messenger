package app.model

trait Message

object Message {

  case class Authorize(id: Long, password: String)

  case class IncomingTextMessage(conversation: Long, timestamp: Long, text: String)

  case class NormalizedTextMessage(conversation: Long, timestamp: Long, text: String, author: Long)

  def normalize(msg: IncomingTextMessage, author: Long): NormalizedTextMessage =
    NormalizedTextMessage(msg.conversation, msg.timestamp, msg.text, author)

  case class NormTextMessageVector(messages: Vector[NormalizedTextMessage])

  case class Sync(timestamp: Long)

  case class SyncConversations()
}
