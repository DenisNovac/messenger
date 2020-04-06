package app.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import Message._
import app.model.DatabaseAbstraction.Conversation

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
  implicit val notFoundEncoder: Encoder[NotFound] = deriveEncoder[NotFound]
  implicit val notFoundDecoder: Decoder[NotFound] = deriveDecoder[NotFound]

  implicit val forbiddenEncoder: Encoder[Forbidden] = deriveEncoder[Forbidden]
  implicit val forbiddenDecoder: Decoder[Forbidden] = deriveDecoder[Forbidden]

  implicit val internalErrEncoder: Encoder[InternalServerError] = deriveEncoder[InternalServerError]
  implicit val internalErrDecoder: Decoder[InternalServerError] = deriveDecoder[InternalServerError]

  implicit val unauthEncoder: Encoder[Unauthorized] = deriveEncoder[Unauthorized]
  implicit val unauthDecoder: Decoder[Unauthorized] = deriveDecoder[Unauthorized]

  implicit val conversationE: Encoder[Conversation]   = deriveEncoder[Conversation]
  implicit val conversationD: Decoder[Conversation]   = deriveDecoder[Conversation]
  implicit val conversationsE: Encoder[Conversations] = deriveEncoder[Conversations]
  implicit val conversationsD: Decoder[Conversations] = deriveDecoder[Conversations]
}
