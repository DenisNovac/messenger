package app.model

import java.util.UUID

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

final case class ConversationBody(name: String, admins: Set[Long], mods: Set[Long], participants: Set[Long]) {

  def ++(that: ConversationBody): ConversationBody =
    ConversationBody(
      this.name,
      this.admins ++ that.admins,
      this.mods ++ that.mods,
      this.participants ++ that.participants
    )

}

object ConversationBody {
  implicit val enc: Encoder[ConversationBody] = deriveEncoder[ConversationBody]
  implicit val dec: Decoder[ConversationBody] = deriveDecoder[ConversationBody]
}

final case class ConversationLegacy(id: UUID, body: ConversationBody) {

  def ++(that: ConversationLegacy) = {
    require(this.id == that.id)
    ConversationLegacy(this.id, this.body ++ that.body)
  }

  def empty = ConversationLegacy(this.id, ConversationBody(this.body.name, Set(), Set(), Set()))
}

object ConversationLegacy {
  implicit val enc: Encoder[ConversationLegacy] = deriveEncoder[ConversationLegacy]
  implicit val dec: Decoder[ConversationLegacy] = deriveDecoder[ConversationLegacy]
}

final case class ConversationAppNew(conv: Conversation, ptc: List[ConversationParticipant])
final case class Conversation(id: UUID, name: String)
final case class ConversationParticipant(id: UUID, convId: UUID, userId: Long, status: Int)
