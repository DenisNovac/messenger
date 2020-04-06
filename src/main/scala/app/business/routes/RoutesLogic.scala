package app.business.routes

import app.business.AuthorizationSystem
import app.model.Message._
import app.model.DatabaseAbstraction

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
      if (conversations.contains(msg.conversation)) {
        DatabaseAbstraction.putMessage(normalize(msg, user.id))
        StatusCode.Ok.asRight[StatusCode].pure[F]
      } else {
        logger.error(s"Not found conversation ${msg.conversation} for user $user")
        StatusCode.NotFound.asRight[StatusCode].pure[F]
      }

    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      StatusCode.Unauthorized.asLeft[StatusCode].pure[F]
    }

  /**
    * Return only messages from specified in Sync timestamp and only from conversations where this user
    * participates.
    * @param cookie cookie for user identification
    * @param s Sync message
    * @return
    */
  def sync(cookie: Option[String], s: Sync): F[Either[StatusCode, NormTextMessageVector]] =
    if (AuthorizationSystem.isCookieValid(cookie)) {

      val (user, conversations) = DatabaseAbstraction.getUserAndConversations(cookie)

      logger.info(s"Sync request from user $user: $s")

      val messagesSinceSync =
        DatabaseAbstraction.getMessages
          .filter(m => conversations.contains(m.conversation)) // Messages with suitable conversations
          .filter(_.timestamp > s.timestamp)                   // And wanted timestamp

      NormTextMessageVector(messagesSinceSync).asRight[StatusCode].pure[F]

    } else {
      logger.error(s"Invalid cookie dropped: $cookie")
      StatusCode.Unauthorized.asLeft[NormTextMessageVector].pure[F]
    }

}
