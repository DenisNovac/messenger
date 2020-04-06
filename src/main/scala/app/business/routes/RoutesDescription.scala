package app.business.routes

import app.model._
import app.model.Message._
import sttp.model.{CookieValueWithMeta, StatusCode}
import sttp.tapir._
import sttp.tapir.docs.openapi._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml._

/**
  * Tapir routes description with OpenAPI spec generation
  * */
object RoutesDescription extends DataEncoders {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")    // Path
      .out(statusCode) // Output type
      .tag("Util")

  val signIn: Endpoint[Authorize, StatusCode, CookieValueWithMeta, Nothing] =
    endpoint.post
      .in("auth")
      .in(jsonBody[Authorize])
      .out(setCookie("sessionid"))
      .errorOut(statusCode)
      .tag("Util")
      .summary("Sign in with user id and password")

  val send: Endpoint[(Option[String], IncomingTextMessage), StatusCode, StatusCode, Nothing] =
    endpoint.post
      .in("send")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[IncomingTextMessage])
      .out(statusCode)
      .errorOut(statusCode)
      .tag("Messenger")
      .summary("Send message to some conversation")

  val sync: Endpoint[(Option[String], Sync), StatusCode, NormTextMessageVector, Nothing] =
    endpoint.post
      .in("sync")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[Sync])
      .out(jsonBody[NormTextMessageVector])
      .errorOut(statusCode)
      .tag("Messenger")
      .summary("Get all messages from specified time from server")

  val authTest: Endpoint[Option[String], StatusCode, StatusCode, Nothing] =
    endpoint.get
      .in("authTest")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .out(statusCode)
      .errorOut(statusCode)
      .tag("Util")
      .summary("Test if cookie is valid")

  /**
    * List of endpoints for generating OpenAPI doc
    * */
  private val openApi: OpenAPI = List(
    health,
    send,
    sync,
    signIn,
    authTest
  ).toOpenAPI(
    "Scala Tapir Messenger",
    "0.0.1" +
      ""
  )

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
