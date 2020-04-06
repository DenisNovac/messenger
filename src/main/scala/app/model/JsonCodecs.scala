package app.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import Message._

trait JsonCodecs {
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

  implicit val addToConvEncoder: Encoder[AddToConversation] = deriveEncoder[AddToConversation]
  implicit val addToConvDecoder: Decoder[AddToConversation] = deriveDecoder[AddToConversation]

  /**
    * Errors codecs
    */
  implicit val notFoundEncoder: Encoder.AsObject[NotFound] = deriveEncoder[NotFound]
  implicit val notFoundDecoder: Decoder[NotFound]          = deriveDecoder[NotFound]

  implicit val forbiddenEncoder: Encoder.AsObject[Forbidden] = deriveEncoder[Forbidden]
  implicit val forbiddenDecoder: Decoder[Forbidden]          = deriveDecoder[Forbidden]

  implicit val internalErrEncoder: Encoder.AsObject[InternalServerError] = deriveEncoder[InternalServerError]
  implicit val internalErrDecoder: Decoder[InternalServerError]          = deriveDecoder[InternalServerError]

  implicit val unauthEncoder: Encoder.AsObject[Unauthorized] = deriveEncoder[Unauthorized]
  implicit val unauthDecoder: Decoder[Unauthorized]          = deriveDecoder[Unauthorized]
}
