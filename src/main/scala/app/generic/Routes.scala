package app.generic

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.docs.openapi._       // for toOpenApi
import sttp.tapir.openapi.circe.yaml._ // for toYaml

object Routes {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")    // Path
      .out(statusCode) // Output type

  // GET /hello?name=...
  val hello: Endpoint[String, Unit, String, Nothing] =
    endpoint.get
      .in("hello")
      .in(query[String]("name")) // Parameter
      .out(stringBody)

  /** List of endpoints for generating OpenAPI doc */
  private val openApi: OpenAPI = List(
    health,
    hello
  ).toOpenAPI("Messenger", "0.0.1")

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
