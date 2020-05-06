package app.controllers

import app.model.Message.Authorize
import sttp.model.{CookieValueWithMeta, StatusCode}
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{auth, cookie, endpoint, setCookie, statusCode, Endpoint}

object AuthController {

  val signIn: Endpoint[Authorize, StatusCode, CookieValueWithMeta, Nothing] =
    endpoint.post
      .in("auth")
      .in(jsonBody[Authorize])
      .out(setCookie("sessionid"))
      .errorOut(statusCode)
      .tag("Auth")
      .summary("Sign in with user id and password")
      .errorOut(
        statusCode(StatusCode.Forbidden).description("Invalid user ID or password")
      )

  val authTest: Endpoint[Option[String], StatusCode, StatusCode, Nothing] =
    endpoint.get
      .in("authTest")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .out(statusCode)
      .errorOut(statusCode)
      .tag("Auth")
      .summary("Test if cookie is valid")
}
