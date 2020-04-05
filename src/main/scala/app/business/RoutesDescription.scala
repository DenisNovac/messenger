package app.business

import app.model.{DataEncoders, Message}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.circe.yaml._
import sttp.tapir.json.circe._

object RoutesDescription extends DataEncoders {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")    // Path
      .out(statusCode) // Output type

  val hello: Endpoint[String, Unit, String, Nothing] =
    endpoint.get
      .in("hello")
      .in(query[String]("name")) // Parameter
      .out(stringBody)

  val test: Endpoint[Unit, Unit, Message, Nothing] =
    endpoint.get
      .in("test")
      .out(jsonBody[Message])

  /**
    * List of endpoints for generating OpenAPI doc
    * */
  private val openApi: OpenAPI = List(
    health,
    hello,
    test
  ).toOpenAPI("Messenger", "0.0.1")

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
