package app.api.endpoints

import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml._

object OpenApiEndpoint {

  /**
    * List of endpoints for generating OpenAPI doc
    * */
  private val openApi: OpenAPI = List(
    UtilEndpoints.health,
    AuthEndpoints.signIn,
    AuthEndpoints.authTest,
    MessagingEndpoints.send,
    MessagingEndpoints.addToConversation,
    MessagingEndpoints.conversations,
    MessagingEndpoints.sync
  ).toOpenAPI(
    "Scala Tapir Messenger",
    "0.0.1"
  )

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
