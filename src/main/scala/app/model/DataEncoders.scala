package app.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import Message._

trait DataEncoders {
  implicit val authEncoder: Encoder[Authorize] = deriveEncoder[Authorize]
  implicit val authDecoder: Decoder[Authorize] = deriveDecoder[Authorize]

  implicit val incMessageEncoder: Encoder[IncomingTextMessage] = deriveEncoder[IncomingTextMessage]
  implicit val incMessageDecoder: Decoder[IncomingTextMessage] = deriveDecoder[IncomingTextMessage]

  implicit val syncEncoder: Encoder[Sync] = deriveEncoder[Sync]
  implicit val syncDecoder: Decoder[Sync] = deriveDecoder[Sync]

  implicit val normMessageEncoder: Encoder[NormalizedTextMessage]  = deriveEncoder[NormalizedTextMessage]
  implicit val normMessageDecoder: Decoder[NormalizedTextMessage]  = deriveDecoder[NormalizedTextMessage]
  implicit val messagesListEncoder: Encoder[NormTextMessageVector] = deriveEncoder[NormTextMessageVector]
  implicit val messagesListDecoder: Decoder[NormTextMessageVector] = deriveDecoder[NormTextMessageVector]
}
