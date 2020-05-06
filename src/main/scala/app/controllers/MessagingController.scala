package app.controllers

import app.model.Message._
import app.model._
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{auth, cookie, endpoint, oneOf, statusCode, statusMapping, Endpoint}

object MessagingController extends JsonCodecs {

  val send: Endpoint[(Option[String], IncomingTextMessage), ErrorInfo, StatusCode, Nothing] =
    endpoint.post
      .in("send")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[IncomingTextMessage])
      .out(statusCode)
      .errorOut(
        oneOf(
          statusMapping(StatusCode.NotFound, jsonBody[NotFound].description("Conversation not found")),
          statusMapping(
            StatusCode.Unauthorized,
            jsonBody[Unauthorized].description("Cookie timed out or does not exists")
          )
        )
      )
      .tag("Messenger")
      .summary("Send message to some conversation")

  val addToConversation: Endpoint[(Option[String], AddToConversation), ErrorInfo, StatusCode, Nothing] =
    endpoint.post
      .in("addToConversation")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[AddToConversation])
      .out(statusCode)
      .errorOut(
        oneOf(
          statusMapping(StatusCode.NotFound, jsonBody[NotFound].description("User or Conversation was not found")),
          statusMapping(
            StatusCode.Unauthorized,
            jsonBody[Unauthorized].description("Cookie timed out or does not exists")
          ),
          statusMapping(
            StatusCode.InternalServerError,
            jsonBody[InternalServerError].description("More than one conversation with such id")
          ),
          statusMapping(StatusCode.Forbidden, jsonBody[Forbidden].description("No privileges for this conversation"))
        )
      )
      .tag("Messenger")
      .summary("Add user to conversation where admin")

  /**
    * For some reason you need TWO .errorOut for .get methods and only one for Post (see signIn)
    */
  val conversations: Endpoint[Option[String], StatusCode, Conversations, Nothing] =
    endpoint.get
      .in("conversations")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .out(jsonBody[Conversations])
      .tag("Messenger")
      .summary("List of user's active conversations")
      .errorOut(statusCode)
      .errorOut(
        statusCode(StatusCode.Unauthorized).description("Cookie is invalid or timed out")
      )

  val sync: Endpoint[(Option[String], Sync), StatusCode, NormTextMessageVector, Nothing] =
    endpoint.post
      .in("sync")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[Sync])
      .out(jsonBody[NormTextMessageVector])
      .errorOut(statusCode)
      .tag("Messenger")
      .summary("Get all messages from specified time from server")
}
