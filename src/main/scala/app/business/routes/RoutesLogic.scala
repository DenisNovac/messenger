package app.business.routes

import app.business.AuthorizationSystem
import app.model.Message._
import app.model.{DatabaseAbstraction, ErrorInfo, Forbidden, InternalServerError, NotFound, Unauthorized}
import app.model.DatabaseAbstraction.{Conversation, User}
import cats.Monad
import cats.syntax.applicative._
import cats.syntax.either._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.{CookieValueWithMeta, StatusCode}

/**
  * Logic is separate from routes definitions. It is initialized by server implementation.
  * It may be IO (http4s) or Future (Akka Http)
  */
class RoutesLogic[F[_]: Monad] extends LazyLogging {

  /** Returns 200 */
  def health: F[Either[Unit, StatusCode]] =
    StatusCode.Ok
      .asRight[Unit]
      .pure[F]

  /** Authorization method issues cookie for users */
  def signIn(authMsg: Authorize): F[Either[StatusCode, CookieValueWithMeta]] =
    AuthorizationSystem.authorize(authMsg) match {
      case Some(value) =>
        logger.info(s"User ${authMsg.id} successfully authorized")
        value.asRight[StatusCode].pure[F]
      case None =>
        logger.warn(s"User ${authMsg.id} couldn't authorize")
        StatusCode.Forbidden.asLeft[CookieValueWithMeta].pure[F]
    }

  /** Validate cookie */
  def testAuth(cookie: Option[String]): F[Either[StatusCode, StatusCode]] =
    if (AuthorizationSystem.isCookieValid(cookie)) {
      StatusCode.Ok.asRight[StatusCode].pure[F]
    } else {
      StatusCode.Unauthorized.asLeft[StatusCode].pure[F]
    }

  /**
    * Send messages only if user authorized and participates in conversation
    * @param cookie Cookie for user identification
    * @param msg Message
    * @return
    */
  def send(cookie: Option[String], msg: IncomingTextMessage): F[Either[StatusCode, StatusCode]] =
    if (AuthorizationSystem.isCookieValid(cookie)) {

      val (user, conversations) = DatabaseAbstraction.getUserAndConversations(cookie)

      // check if this messages is for suitable conversation
      if (conversations.map(_.id).contains(msg.conversation)) {
        DatabaseAbstraction.putMessage(normalize(msg, user.id))
        StatusCode.Ok.asRight[StatusCode].pure[F]
      } else {
        logger.error(s"Not found conversation ${msg.conversation} for user ${user.id}")
        StatusCode.NotFound.asRight[StatusCode].pure[F]
      }

    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      StatusCode.Unauthorized.asLeft[StatusCode].pure[F]
    }

  /**
    * Return only messages from specified in Sync timestamp and only from conversations where this user
    * participates.
    * @param cookie Cookie for user identification
    * @param s Sync message
    * @return
    */
  def sync(cookie: Option[String], s: Sync): F[Either[StatusCode, NormTextMessageVector]] =
    if (AuthorizationSystem.isCookieValid(cookie)) {

      val (user, conversations) = DatabaseAbstraction.getUserAndConversations(cookie)

      logger.info(s"Sync request from user ${user.id}: $s")

      val messagesSinceSync =
        DatabaseAbstraction.getMessages
          .filter(m => conversations.map(_.id).contains(m.conversation)) // Messages from conversation of this user
          .filter(_.timestamp > s.timestamp)                             // And wanted timestamp

      NormTextMessageVector(messagesSinceSync).asRight[StatusCode].pure[F]

    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      StatusCode.Unauthorized.asLeft[NormTextMessageVector].pure[F]
    }

  /**
    * List of user's active conversations
    * */
  def conversationsList(cookie: Option[String]): F[Either[StatusCode, Conversations]] =
    if (AuthorizationSystem.isCookieValid(cookie)) {
      val (user, conversations) = DatabaseAbstraction.getUserAndConversations(cookie)

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
    if (AuthorizationSystem.isCookieValid(cookie)) {

      val (maybeAdmin, conversations) = DatabaseAbstraction.getUserAndConversations(cookie)

      (
        conversations.toList.filter(_.id == add.conversationId),
        DatabaseAbstraction.getUserById(add.newUserId)
      ) match {

        /** Conversation (only one) and user exists, performed by admin of conversation */
        case (conversation :: Nil, Some(newUser)) if conversation.admins.contains(maybeAdmin.id) =>
          if (conversation.participants.contains(newUser.id)) {
            StatusCode.Ok.asRight[ErrorInfo].pure[F] // just OK without welcome message, user is already here
          } else {
            logger.info(
              s"User ${maybeAdmin.prettyName} adds user ${newUser.prettyName} to conversation ${add.conversationId}"
            )

            val welcome = normalize(
              IncomingTextMessage(
                add.conversationId,
                1,
                s"${maybeAdmin.prettyName} adds user ${newUser.prettyName} to this conversation"
              ),
              maybeAdmin.id
            )

            DatabaseAbstraction.putMessage(welcome)

            // New list of participants with new user
            val newParticipantsList = conversation.participants :+ newUser.id
            DatabaseAbstraction.updateConversations(
              add.conversationId,
              Conversation(add.conversationId, conversation.name, conversation.admins, newParticipantsList)
            )
            StatusCode.Ok.asRight[ErrorInfo].pure[F]
          }

        /** User is not an admin */
        case (conversation :: Nil, Some(newUser)) =>
          logger.error(
            s"${maybeAdmin.prettyName} tried to add user ${add.newUserId} to conversation ${add.conversationId} without privileges"
          )
          val r: ErrorInfo = Forbidden("No privileges to add users in this conversation")
          r.asLeft[StatusCode].pure[F]

        /** No such conversation */
        case (Nil, _) =>
          logger.error(
            s"User ${maybeAdmin.id} tried to add user ${add.newUserId} to not existing conversation ${add.conversationId}"
          )
          val r: ErrorInfo = NotFound("Conversation not found")
          r.asLeft[StatusCode].pure[F]

        /** No such user */
        case (_, None) =>
          logger.info(
            s"${maybeAdmin.prettyName} tried to add non-existing user ${add.newUserId} to conversation ${add.conversationId}"
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
      StatusCode.Unauthorized.asLeft[StatusCode].pure[F]
      val r: ErrorInfo = Unauthorized("Cookie timed out or invalid")
      r.asLeft[StatusCode].pure[F]
    }

}
