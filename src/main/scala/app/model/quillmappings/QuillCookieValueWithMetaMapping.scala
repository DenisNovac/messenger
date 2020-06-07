package app.model.quillmappings

import io.getquill.MappedEncoding
import sttp.model.CookieValueWithMeta
import io.circe.syntax._

object QuillCookieValueWithMetaMapping {
  import app.model.AuthorizedSession.{cookieDec, cookieEnc}

  implicit val cookieEncoding: MappedEncoding[CookieValueWithMeta, String] =
    MappedEncoding[CookieValueWithMeta, String] { cookie =>
      cookie.asJson.toString
    }

  implicit val cookieDecoding: MappedEncoding[String, CookieValueWithMeta] =
    MappedEncoding[String, CookieValueWithMeta] { cookie =>
      cookie.asJson.as[CookieValueWithMeta] match {
        case Right(meta) => meta
      }
    }
}
