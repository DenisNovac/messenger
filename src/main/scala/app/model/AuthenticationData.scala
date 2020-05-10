package app.model

import java.time.Instant

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

final case class CookieBody(user: User, expires: Option[Instant], body: CookieValueWithMeta) extends AuthenticationData

object CookieBody {
  implicit val encVal: Encoder[CookieValueWithMeta] = deriveEncoder[CookieValueWithMeta]
  implicit val decVal: Decoder[CookieValueWithMeta] = deriveDecoder[CookieValueWithMeta]

  implicit val enc: Encoder[CookieBody] = deriveEncoder[CookieBody]
  implicit val dec: Decoder[CookieBody] = deriveDecoder[CookieBody]
}
