package app.controllers

import sttp.tapir.docs.openapi._
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.openapi.circe.yaml._

object OpenApiController {

  /**
    * List of endpoints for generating OpenAPI doc
    * */
  private val openApi: OpenAPI = List(
    UtilController.health,
    AuthController.signIn,
    AuthController.authTest,
    MessagingController.send,
    MessagingController.addToConversation,
    MessagingController.conversations,
    MessagingController.sync
  ).toOpenAPI(
    "Scala Tapir Messenger",
    "0.0.1"
  )

  /** One only need to create with this openApiYml for each server type and it just works */
  val openApiYml: String = openApi.toYaml
}
