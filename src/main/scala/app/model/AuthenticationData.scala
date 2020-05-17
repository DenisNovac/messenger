package app.model

import java.time.Instant
import java.util.UUID

import doobie.util.Put
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import sttp.model.CookieValueWithMeta

trait AuthenticationData

final case class User(id: Long, name: String, password: String) extends AuthenticationData {
  def prettyName: String = s"$name#$id" // name like Sam#111
}

object User {
  implicit val enc: Encoder[User] = deriveEncoder[User]
  implicit val dec: Decoder[User] = deriveDecoder[User]
}

final case class Cookie(id: UUID, userid: Long, expires: Option[Instant], body: CookieValueWithMeta)
    extends AuthenticationData


object Cookie {
  implicit val encVal: Encoder[CookieValueWithMeta] = deriveEncoder[CookieValueWithMeta]
  implicit val decVal: Decoder[CookieValueWithMeta] = deriveDecoder[CookieValueWithMeta]

  implicit val enc: Encoder[Cookie] = deriveEncoder[Cookie]
  implicit val dec: Decoder[Cookie] = deriveDecoder[Cookie]
}


