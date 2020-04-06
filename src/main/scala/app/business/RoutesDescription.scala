package app.business

import app.model.{DataEncoders, Message, Session, Sync}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.json.circe._

object RoutesDescription extends DataEncoders {

  val health: Endpoint[Unit, Unit, Message, Nothing] =
    endpoint.get
      .in("health")           // Path
      .out(jsonBody[Message]) // Output type

  val send: Endpoint[Message, Unit, StatusCode, Nothing] =
    endpoint.post
      .in("send")
      .in(jsonBody[Message])
      .out(statusCode)

  val sync: Endpoint[Sync, Unit, Session, Nothing] =
    endpoint.post
      .in("sync")
      .in(jsonBody[Sync])
      .out(jsonBody[Session])

  val getAuth =
    endpoint.get
      .in("auth")
      .out(setCookie("sessionid"))
      .errorOut(statusCode)

  val authTest: Endpoint[Option[String], StatusCode, StatusCode, Nothing] =
    endpoint.get
      .in("authTest")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .out(statusCode)
      .errorOut(statusCode)

  /**
    * List of endpoints for generating OpenAPI doc
    * */
  private val openApi: OpenAPI = List(
    health,
    send,
    sync,
    getAuth,
    authTest
  ).toOpenAPI("Messenger", "0.0.1")

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
