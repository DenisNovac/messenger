package app.api.endpoints

import app.model._
import sttp.model.StatusCode
import sttp.tapir.json.circe.jsonBody
import sttp.tapir.{auth, cookie, endpoint, oneOf, statusCode, statusMapping, Endpoint}

object MessagingEndpoints {

  val send: Endpoint[(Option[String], IncomingTextMessage), StatusCode, StatusCode, Nothing] =
    endpoint.post
      .in("send")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[IncomingTextMessage])
      .out(statusCode)
      .errorOut(
        statusCode
          .description(StatusCode.NotFound, "Conversation not found")
          .description(StatusCode.Unauthorized, "User not found or cookie is invalid")
      )
      .tag("Messaging")
      .summary("Send message to some conversation")

  val addToConversation: Endpoint[(Option[String], AddToConversation), StatusCode, StatusCode, Nothing] =
    endpoint.post
      .in("addToConversation")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[AddToConversation])
      .out(statusCode)
      .errorOut(
        statusCode
          .description(StatusCode.NotFound, "User or conversation for adding not found")
          .description(StatusCode.Forbidden, "User is not an admin of conversation")
          .description(StatusCode.Unauthorized, "User not found or cookie is invalid")
      )
      .tag("Messaging")
      .summary("Add user to conversation where admin")

  /**
    * For some reason you need TWO .errorOut for .get methods and only one for Post (see signIn)
    */
  val conversations: Endpoint[Option[String], StatusCode, Conversations, Nothing] =
    endpoint.get
      .in("conversations")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .out(jsonBody[Conversations])
      .tag("Messaging")
      .summary("List of user's active conversations")
      .errorOut(
        statusCode.description(StatusCode.Unauthorized, "Cookie is invalid or timed out")
      )

  val sync: Endpoint[(Option[String], Sync), StatusCode, NormalizedTextMessageVector, Nothing] =
    endpoint.post
      .in("sync")
      .in(auth.apiKey(cookie[Option[String]]("sessionid")))
      .in(jsonBody[Sync])
      .out(jsonBody[NormalizedTextMessageVector])
      .errorOut(statusCode)
      .tag("Messaging")
      .summary("Get all messages from specified time from server")
}
