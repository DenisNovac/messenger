package app.model

import io.circe._
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

sealed trait Data
final case class Message(timestamp: Int, user: String, text: String) extends Data
final case class Sync(timestamp: Int)                                extends Data
final case class Session(messages: Vector[Message])                  extends Data
final case class Authorize(id: String, password: String)             extends Data

/**
  * Trait with JSON-encoders and decoders.
  * This trait contains proofs for JSON constructors such as jsonBody[Message]
  * */
trait DataEncoders {
  implicit val messageEncoder: Encoder.AsObject[Message] = deriveEncoder[Message]
  implicit val messageDecoder: Decoder[Message]          = deriveDecoder[Message]

  implicit val syncEncoder: Encoder.AsObject[Sync] = deriveEncoder[Sync]
  implicit val syncDecoder: Decoder[Sync]          = deriveDecoder[Sync]

  implicit val sessionEncoder: Encoder.AsObject[Session] = deriveEncoder[Session]
  implicit val sessionDecoder: Decoder[Session]          = deriveDecoder[Session]

  implicit val authEncoder: Encoder.AsObject[Authorize] = deriveEncoder[Authorize]
  implicit val authDecoder: Decoder[Authorize]          = deriveDecoder[Authorize]
}
