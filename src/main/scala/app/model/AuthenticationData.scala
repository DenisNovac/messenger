package app.model

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.{Date, UUID}

import doobie.util.{Read, Write}
import doobie.implicits.legacy.instant._
import app.model.PostgresJsonMapping._
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
  /*
  val ctx = app.init.Init.postgres.quillContext
  import ctx._

  implicit val cookieEncoding: ctx.MappedEncoding[CookieValueWithMeta, String] =
    ctx.MappedEncoding[CookieValueWithMeta, String] { cookie =>
      cookie.asJson.toString
    }

  implicit val cookieDecoding: ctx.MappedEncoding[String, CookieValueWithMeta] =
    ctx.MappedEncoding[String, CookieValueWithMeta] { cookie =>
      cookie.asJson.as[CookieValueWithMeta] match {
        case Right(meta) => meta
      }
    }

  implicit val encodeInstant =
    ctx.MappedEncoding[Instant, LocalDateTime](i => LocalDateTime.ofInstant(i, ZoneOffset.UTC))
  implicit val decodeInstant = ctx.MappedEncoding[LocalDateTime, Instant](l => l.toInstant(ZoneOffset.UTC))*/

  /*implicit val instantEncoding: ctx.MappedEncoding[Instant, Date] = ctx.MappedEncoding[Instant, Date](Date.from)
  implicit val instantDecoding: ctx.MappedEncoding[Date, Instant] = ctx.MappedEncoding[Date, Instant](_.toInstant())*/

  /*implicit val cookieGet: Read[AuthorizedSession] =
    Read[(UUID, Long, Option[Instant], Json)]
      .map {
        case (uuid, l, maybeInstant, metaJson) =>
          metaJson.as[CookieValueWithMeta] match {
            case Right(meta) => AuthorizedSession(uuid, l, maybeInstant, meta)
          }

      }

  implicit val cookieWrite: Write[AuthorizedSession] =
    Write[(UUID, Long, Option[Instant], Json)].contramap(c => (c.id, c.userid, c.expires, c.body.asJson))*/
}
