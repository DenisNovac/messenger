package app.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

trait Message

final case class Authorize(id: Long, password: String) extends Message

object Authorize {
  implicit val enc: Encoder[Authorize] = deriveEncoder[Authorize]
  implicit val dec: Decoder[Authorize] = deriveDecoder[Authorize]
}

final case class AddToConversation(conversationId: Long, newUserId: Long) extends Message

object AddToConversation {
  implicit val enc: Encoder[AddToConversation] = deriveEncoder[AddToConversation]
  implicit val dec: Decoder[AddToConversation] = deriveDecoder[AddToConversation]

}

final case class Sync(timestamp: Long) extends Message

object Sync {
  implicit val enc: Encoder[Sync] = deriveEncoder[Sync]
  implicit val dec: Decoder[Sync] = deriveDecoder[Sync]
}

final case class SyncConversations() extends Message

final case class IncomingTextMessage(conversation: Long, timestamp: Long, text: String) extends Message

object IncomingTextMessage {
  implicit val enc: Encoder[IncomingTextMessage] = deriveEncoder[IncomingTextMessage]
  implicit val dec: Decoder[IncomingTextMessage] = deriveDecoder[IncomingTextMessage]

}

final case class NormalizedTextMessage(conversation: Long, timestamp: Long, text: String, author: Long) extends Message

object NormalizedTextMessage {
  implicit val enc: Encoder[NormalizedTextMessage] = deriveEncoder[NormalizedTextMessage]
  implicit val dec: Decoder[NormalizedTextMessage] = deriveDecoder[NormalizedTextMessage]

  def normalize(msg: IncomingTextMessage, author: Long): NormalizedTextMessage =
    NormalizedTextMessage(msg.conversation, msg.timestamp, msg.text, author)
}

final case class NormalizedTextMessageVector(messages: Vector[NormalizedTextMessage])

object NormalizedTextMessageVector {
  implicit val enc: Encoder[NormalizedTextMessageVector] = deriveEncoder[NormalizedTextMessageVector]
  implicit val dec: Decoder[NormalizedTextMessageVector] = deriveDecoder[NormalizedTextMessageVector]
}

final case class Conversations(userConversations: Vector[Conversation]) extends Message

object Conversations {
  implicit val enc: Encoder[Conversations] = deriveEncoder[Conversations]
  implicit val dec: Decoder[Conversations] = deriveDecoder[Conversations]
}
