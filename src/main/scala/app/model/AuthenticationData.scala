package app.model

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

import doobie.util.{Read, Write}
import doobie.implicits.legacy.instant._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.quill.DoobieContext
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import io.getquill.NamingStrategy
import sttp.model.CookieValueWithMeta

trait AuthenticationData

final case class MessengerUser(id: Long, name: String, password: String) extends AuthenticationData {
  def prettyName: String = s"$name#$id" // name like Sam#111
}

object MessengerUser {
  implicit val enc: Encoder[MessengerUser] = deriveEncoder[MessengerUser]
  implicit val dec: Decoder[MessengerUser] = deriveDecoder[MessengerUser]
}

final case class AuthorizedSession(id: UUID, userId: Long, expires: Option[Instant], body: CookieValueWithMeta)
    extends AuthenticationData

object AuthorizedSession {

  implicit val enc: Encoder[AuthorizedSession] = deriveEncoder[AuthorizedSession]
  implicit val dec: Decoder[AuthorizedSession] = deriveDecoder[AuthorizedSession]

  implicit val cookieEnc: Encoder[CookieValueWithMeta] = deriveEncoder[CookieValueWithMeta]
  implicit val cookieDec: Decoder[CookieValueWithMeta] = deriveDecoder[CookieValueWithMeta]
}
