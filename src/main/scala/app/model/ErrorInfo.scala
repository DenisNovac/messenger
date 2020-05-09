package app.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

/**
  * Type for providing an additional info for errors in some complicated requests
  */
sealed trait ErrorInfo extends Product with Serializable

final case class NotFound(additional: String) extends ErrorInfo

object NotFound {
  implicit val enc: Encoder[NotFound] = deriveEncoder[NotFound]
  implicit val dec: Decoder[NotFound] = deriveDecoder[NotFound]
}

final case class Forbidden(additional: String) extends ErrorInfo

object Forbidden {
  implicit val enc: Encoder[Forbidden] = deriveEncoder[Forbidden]
  implicit val dec: Decoder[Forbidden] = deriveDecoder[Forbidden]
}

final case class InternalServerError(additional: String) extends ErrorInfo

object InternalServerError {
  implicit val enc: Encoder[InternalServerError] = deriveEncoder[InternalServerError]
  implicit val dec: Decoder[InternalServerError] = deriveDecoder[InternalServerError]

}

final case class Unauthorized(additional: String = "Cookie timed out or invalid") extends ErrorInfo

object Unauthorized {
  implicit val enc: Encoder[Unauthorized] = deriveEncoder[Unauthorized]
  implicit val dec: Decoder[Unauthorized] = deriveDecoder[Unauthorized]
}
