package app.business

import app.model.{DataEncoders, Message}
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

  /**
    * List of endpoints for generating OpenAPI doc
    * */
  private val openApi: OpenAPI = List(
    health
  ).toOpenAPI("Messenger", "0.0.1")

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
