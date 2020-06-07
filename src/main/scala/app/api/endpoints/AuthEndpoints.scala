package app.api.endpoints

import app.model.{Authorize, AuthorizedSession}
import sttp.model.{CookieValueWithMeta, StatusCode}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir._

object AuthEndpoints {

  val signIn: Endpoint[Authorize, StatusCode, CookieValueWithMeta, Nothing] =
    endpoint.post
      .in("auth")
      .in(jsonBody[Authorize])
      .out(setCookie("sessionid"))
      .tag("Auth")
      .summary("Sign in with user id and password")
      .errorOut(
        statusCode
          .description(StatusCode.Forbidden, "User ID not found or password is incorrect")
          .description(StatusCode.ServiceUnavailable, "Unknown database error")
      )

  val authTest: Endpoint[Option[String], StatusCode, AuthorizedSession, Nothing] =
    endpoint.get
      .in("authTest")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .out(jsonBody[AuthorizedSession])
      .errorOut(statusCode)
      .tag("Auth")
      .summary("Test if cookie is valid")
}
