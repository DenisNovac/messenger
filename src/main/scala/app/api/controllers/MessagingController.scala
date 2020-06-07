package app.api.controllers

import app.api.services.AuthService
import app.api.services.db.{InMemoryDatabase, PostgresService}
import app.model._
import app.model.NormalizedTextMessage.normalize

import cats.Monad
import cats.data.OptionT
import cats.effect.IO
import cats.syntax.either._

import com.typesafe.scalalogging.LazyLogging
import sttp.model.StatusCode

class MessagingController[F[_]: Monad] extends LazyLogging {

  /**
    * Send messages only if user authorized and participates in conversation
    *
    * @param cookie Cookie for user identification
    * @param msg    Message
    * @return
    */
  def send(cookie: Option[String], msg: IncomingTextMessage): IO[Either[StatusCode, StatusCode]] =
    AuthService.authorizedAction(cookie) { token =>
      val convId = msg.conversation

      for {
        convs <- PostgresService.getUserConversations(token)
      } yield {
        if (convs.map(_.id).contains(convId)) {
          InMemoryDatabase.putMessage(normalize(msg, token.userId))
          StatusCode.Ok.asRight[StatusCode]
        } else {
          StatusCode.NotFound.asLeft[StatusCode]
        }
      }
    }

  /**
    * Return only messages from specified in Sync timestamp and only from conversations where this user
    * participates.
    *
    * @param cookie Cookie for user identification
    * @param s      Sync message
    * @return
    */
  def sync(cookie: Option[String], s: Sync): IO[Either[StatusCode, NormalizedTextMessageVector]] =
    AuthService.authorizedAction(cookie) { token =>
      for {
        conversations <- PostgresService.getUserConversations(token)
      } yield {
        val messagesSinceSync = InMemoryDatabase.getMessages
          .filter(m => conversations.map(_.id).contains(m.conversation)) // Messages from conversation of this user
          .filter(_.timestamp > s.timestamp)                             // And wanted timestamp
          .sortWith((msg1, msg2) => msg1.timestamp < msg2.timestamp)

        NormalizedTextMessageVector(messagesSinceSync).asRight[StatusCode]
      }
    }

  /**
    * List of user's active conversations
    **/
  def conversationsList(cookie: Option[String]): IO[Either[StatusCode, Conversations]] =
    AuthService.authorizedAction(cookie) { token =>
      PostgresService.getConversationsWithMeta(token).map(_.asRight[StatusCode])
    }

  /**
    * Add user to conversation. User must be an admin of this conversation.
    *
    * @param cookie Cookie for user identification
    * @param add    AddToConversation message
    * @return
    */
  def addToConversation(cookie: Option[String], add: AddToConversation): IO[Either[StatusCode, StatusCode]] =
    AuthService.authorizedAction(cookie) { token =>
      PostgresService.getConversationsWithMeta(token).flatMap { convs =>
        val maybeAdmin    = token.userId
        val conversations = convs.userConversations

        {
          for {
            newUser <- OptionT(PostgresService.getUserById(add.newUserId))
          } yield conversations.find(_.id == add.conversationId) match {

            case Some(conversation) if conversation.body.admins.contains(maybeAdmin) =>
              if (conversation.body.participants.contains(newUser.id))
                StatusCode.Ok.asRight[StatusCode] // just OK without welcome message, user is already here
              else {

                val welcome = normalize(
                  IncomingTextMessage(
                    add.conversationId,
                    1,
                    s"${maybeAdmin} adds user ${newUser.prettyName} to this conversation"
                  ),
                  maybeAdmin
                )

                InMemoryDatabase.putMessage(welcome)

                // New list of participants with new user
                val newParticipantsList = conversation.body.participants + newUser.id

                PostgresService
                  .updateConversation(
                    add.conversationId,
                    ConversationBody(conversation.body.name, conversation.body.admins, Set(), newParticipantsList)
                  )
                  .unsafeRunSync()

                StatusCode.Ok.asRight[StatusCode]
              }

            // User is not an admin
            case Some(conversation) => StatusCode.Forbidden.asLeft[StatusCode]
          }
        }.getOrElse(StatusCode.NotFound.asLeft[StatusCode])

      }

    }
}
