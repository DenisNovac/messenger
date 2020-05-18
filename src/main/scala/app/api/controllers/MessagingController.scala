package app.api.controllers

import app.api.services.AuthService
import app.api.services.db.InMemoryDatabase
import app.model.{ErrorInfo, Forbidden, InternalServerError, NotFound, Unauthorized}
import app.model._
import app.model.NormalizedTextMessage.normalize
import cats.Monad
import cats.effect.IO
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.applicative._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.StatusCode

class MessagingController[F[_]: Monad] extends LazyLogging {

  /**
    * Send messages only if user authorized and participates in conversation
    * @param cookie Cookie for user identification
    * @param msg Message
    * @return
    */
  def send(cookie: Option[String], msg: IncomingTextMessage): IO[Either[StatusCode, StatusCode]] =
    AuthService.authorizedAction(cookie) { token =>
      val (user, conversations) = InMemoryDatabase.getUserAndConversations(token.id.toString.some)

      if (conversations.map(_.id).contains(msg.conversation)) {
        InMemoryDatabase.putMessage(normalize(msg, user))
        StatusCode.Ok.asRight[StatusCode]
      } else {
        StatusCode.NotFound.asLeft[StatusCode]
      }

    }

  /**
    * Return only messages from specified in Sync timestamp and only from conversations where this user
    * participates.
    * @param cookie Cookie for user identification
    * @param s Sync message
    * @return
    */
  def sync(cookie: Option[String], s: Sync): F[Either[StatusCode, NormalizedTextMessageVector]] =
    if (AuthService.isCookieValid(cookie).unsafeRunSync) {

      val (user, conversations) = InMemoryDatabase.getUserAndConversations(cookie)

      logger.info(s"Sync request from user ${user}: $s")

      val messagesSinceSync =
        InMemoryDatabase.getMessages
          .filter(m => conversations.map(_.id).contains(m.conversation)) // Messages from conversation of this user
          .filter(_.timestamp > s.timestamp)                             // And wanted timestamp
          .sortWith((msg1, msg2) => msg1.timestamp < msg2.timestamp)

      NormalizedTextMessageVector(messagesSinceSync).asRight[StatusCode].pure[F]

    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      StatusCode.Unauthorized.asLeft[NormalizedTextMessageVector].pure[F]
    }

  /**
    * List of user's active conversations
    * */
  def conversationsList(cookie: Option[String]): F[Either[StatusCode, Conversations]] =
    if (AuthService.isCookieValid(cookie).unsafeRunSync) {
      val (user, conversations) = InMemoryDatabase.getUserAndConversations(cookie)

      Conversations(conversations).asRight[StatusCode].pure[F]
    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      StatusCode.Unauthorized.asLeft[Conversations].pure[F]
    }

  /**
    * Add user to conversation. User must be an admin of this conversation.
    * @param cookie Cookie for user identification
    * @param add AddToConversation message
    * @return
    */
  def addToConversation(cookie: Option[String], add: AddToConversation): F[Either[ErrorInfo, StatusCode]] =
    if (AuthService.isCookieValid(cookie).unsafeRunSync) {

      val (maybeAdmin, conversations) = InMemoryDatabase.getUserAndConversations(cookie)

      (
        conversations.toList.filter(_.id == add.conversationId),
        InMemoryDatabase.getUserById(add.newUserId)
      ) match {

        /** Conversation (only one) and user exists, performed by admin of conversation */
        case (conversation :: Nil, Some(newUser)) if conversation.body.admins.contains(maybeAdmin) =>
          if (conversation.body.participants.contains(newUser.id)) {
            StatusCode.Ok.asRight[ErrorInfo].pure[F] // just OK without welcome message, user is already here
          } else {
            logger.info(
              s"User ${maybeAdmin} adds user ${newUser.prettyName} to conversation ${add.conversationId}"
            )

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
            InMemoryDatabase.updateConversation(
              add.conversationId,
              ConversationBody(conversation.body.name, conversation.body.admins, Set(), newParticipantsList)
            )
            StatusCode.Ok.asRight[ErrorInfo].pure[F]
          }

        /** User is not an admin */
        case (conversation :: Nil, Some(newUser)) =>
          logger.error(
            s"${maybeAdmin} tried to add user ${add.newUserId} to conversation ${add.conversationId} without privileges"
          )
          val r: ErrorInfo = Forbidden("No privileges to add users in this conversation")
          r.asLeft[StatusCode].pure[F]

        /** No such conversation */
        case (Nil, _) =>
          logger.error(
            s"User ${maybeAdmin} tried to add user ${add.newUserId} to not existing conversation ${add.conversationId}"
          )
          val r: ErrorInfo = NotFound("Conversation not found")
          r.asLeft[StatusCode].pure[F]

        /** No such user */
        case (_, None) =>
          logger.info(
            s"${maybeAdmin} tried to add non-existing user ${add.newUserId} to conversation ${add.conversationId}"
          )
          val r: ErrorInfo = NotFound("User not found")
          r.asLeft[StatusCode].pure[F]

        /** Multiple conversations with one id */
        case (x :: xs, _) =>
          logger.error(s"Multiple conversations with id ${add.conversationId}")
          val r: ErrorInfo = InternalServerError("Multiple conversations with one id")
          r.asLeft[StatusCode].pure[F]
      }

    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      val r: ErrorInfo = Unauthorized()
      r.asLeft[StatusCode].pure[F]
    }
}
