package app.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ConversationBody(name: String, admins: Vector[Long], participants: Vector[Long])

object ConversationBody {
  implicit val enc: Encoder[ConversationBody] = deriveEncoder[ConversationBody]
  implicit val dec: Decoder[ConversationBody] = deriveDecoder[ConversationBody]
}

final case class Conversation(id: Long, body: ConversationBody)

object Conversation {
  implicit val enc: Encoder[Conversation] = deriveEncoder[Conversation]
  implicit val dec: Decoder[Conversation] = deriveDecoder[Conversation]
}
