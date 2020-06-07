package app.model.quillmappings

import com.typesafe.scalalogging.LazyLogging
import io.getquill.MappedEncoding
import sttp.model.CookieValueWithMeta
import io.circe.syntax._
import io.circe.parser._

object QuillCookieValueWithMetaMapping extends LazyLogging {
  import app.model.AuthorizedSession.{cookieDec, cookieEnc}

  implicit val cookieEncoding: MappedEncoding[CookieValueWithMeta, String] =
    MappedEncoding[CookieValueWithMeta, String] { cookie =>
      cookie.asJson.toString
    }

  implicit val cookieDecoding: MappedEncoding[String, CookieValueWithMeta] =
    MappedEncoding[String, CookieValueWithMeta] { rawJson =>
      parse(rawJson).flatMap(_.as[CookieValueWithMeta]) match {
        case Right(value) => value
        case Left(value) =>
          logger.error(s"""
                          |Couldn't parse raw json:
                          |$rawJson
                          |The error is:
                          |${value.getStackTrace}
                          |""".stripMargin)
          throw value
      }
    }
}
