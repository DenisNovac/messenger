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

final case class ConversationApp(id: UUID, body: ConversationBody) {

  def ++(that: ConversationApp) = {
    require(this.id == that.id)
    ConversationApp(this.id, that.body ++ that.body)
  }
}

object ConversationApp {
  implicit val enc: Encoder[ConversationApp] = deriveEncoder[ConversationApp]
  implicit val dec: Decoder[ConversationApp] = deriveDecoder[ConversationApp]
}

final case class ConversationAppNew(conv: Conversation, ptc: List[ConversationParticipant])
final case class Conversation(id: UUID, name: String)
final case class ConversationParticipant(id: UUID, convId: UUID, userId: Long, status: Int)
