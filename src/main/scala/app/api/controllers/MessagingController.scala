package app.api.controllers

import app.api.services.AuthService
import app.api.services.db.{DatabaseService, InMemoryDatabase, TransactorDatabaseService}
import app.model._
import app.model.NormalizedTextMessage.normalize
import cats.data.OptionT
import cats.effect.{Async, IO}
import com.typesafe.scalalogging.LazyLogging
import sttp.model.StatusCode
import cats.implicits._

class MessagingController[F[_]: Async](authService: AuthService[F], db: DatabaseService[F]) extends LazyLogging {

  /**
    * Send messages only if user authorized and participates in conversation
    *
    * @param cookie Cookie for user identification
    * @param msg    Message
    * @return
    */
  def send(cookie: Option[String], msg: IncomingTextMessage): F[Either[StatusCode, StatusCode]] =
    authService.authorizedAction(cookie) { token =>
      val convId = msg.conversation

      for {
        convs <- db.getUserConversations(token)
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
  def sync(cookie: Option[String], s: Sync): F[Either[StatusCode, NormalizedTextMessageVector]] =
    authService.authorizedAction(cookie) { token =>
      for {
        conversations <- db.getUserConversations(token)
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
  def conversationsList(cookie: Option[String]): F[Either[StatusCode, Conversations]] =
    authService.authorizedAction(cookie) { token =>
      db.getConversationsWithMeta(token).map(_.asRight[StatusCode])
    }

  /**
    * Add user to conversation. User must be an admin of this conversation.
    *
    * @param cookie Cookie for user identification
    * @param add    AddToConversation message
    * @return
    */
  def addToConversation(cookie: Option[String], add: AddToConversation): F[Either[StatusCode, StatusCode]] =
    authService.authorizedAction(cookie) { token =>
      {
        for {
          convs   <- OptionT.liftF[F, Conversations](db.getConversationsWithMeta(token))
          newUser <- OptionT[F, MessengerUser](db.getUserById(add.newUserId))
          controlled <- OptionT.fromOption[F](
                         convs.userConversations.find(_.id == add.conversationId)
                       )
          if controlled.body.admins.contains(token.userId)
          allUsers = controlled.body.participants ++ controlled.body.admins ++ controlled.body.mods
        } yield
          if (allUsers.contains(newUser.id)) {
            StatusCode.Ok.asRight[StatusCode].pure[F]
          } else {
            val welcome = normalize(
              IncomingTextMessage(
                add.conversationId,
                1,
                s"${token.userId} adds user ${newUser.prettyName} to this conversation"
              ),
              token.userId
            )

            InMemoryDatabase.putMessage(welcome)

            db.addParticipantsToConversation(
              add.conversationId,
              newUser.id
            ) >> StatusCode.Ok.asRight[StatusCode].pure[F]
          }
      }.getOrElse(StatusCode.NotFound.asLeft[StatusCode].pure[F]).flatten
    }
}
