package app.model

import java.time.Instant
import java.util.UUID

import doobie.util.{Read, Write}
import doobie.implicits.legacy.instant._
import app.model.PostgresJsonMapping._
import doobie.implicits._
import doobie.postgres.implicits._

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax._
import sttp.model.CookieValueWithMeta

trait AuthenticationData

final case class MessengerUser(id: Long, name: String, password: String) extends AuthenticationData {
  def prettyName: String = s"$name#$id" // name like Sam#111
}

object MessengerUser {
  implicit val enc: Encoder[MessengerUser] = deriveEncoder[MessengerUser]
  implicit val dec: Decoder[MessengerUser] = deriveDecoder[MessengerUser]
}

final case class Cookie(id: UUID, userid: Long, expires: Option[Instant], body: CookieValueWithMeta)
    extends AuthenticationData

object Cookie {
  implicit val encVal: Encoder[CookieValueWithMeta] = deriveEncoder[CookieValueWithMeta]
  implicit val decVal: Decoder[CookieValueWithMeta] = deriveDecoder[CookieValueWithMeta]

  implicit val enc: Encoder[Cookie] = deriveEncoder[Cookie]
  implicit val dec: Decoder[Cookie] = deriveDecoder[Cookie]

  implicit val cookieGet: Read[Cookie] =
    Read[(UUID, Long, Option[Instant], Json)]
      .map {
        case (uuid, l, maybeInstant, metaJson) =>
          metaJson.as[CookieValueWithMeta] match {
            case Right(meta) => Cookie(uuid, l, maybeInstant, meta)
          }

      }

  implicit val cookieWrite: Write[Cookie] =
    Write[(UUID, Long, Option[Instant], Json)].contramap(c => (c.id, c.userid, c.expires, c.body.asJson))
}
