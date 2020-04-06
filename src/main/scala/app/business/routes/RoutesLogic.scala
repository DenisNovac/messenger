package app.business.routes

import app.business.AuthorizationSystem
import app.model._
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

  var session: Vector[Message] = Vector()

  /** Authorization method issues cookie for users */
  def signIn(authMsg: Authorize): F[Either[StatusCode, CookieValueWithMeta]] =
    AuthorizationSystem.authorize(authMsg) match {
      case Some(value) => value.asRight[StatusCode].pure[F]
      case None        => StatusCode.Forbidden.asLeft[CookieValueWithMeta].pure[F]
    }

  /** Validate cookie */
  def testAuth(cookie: Option[String]): F[Either[StatusCode, StatusCode]] =
    if (AuthorizationSystem.isCookieValid(cookie)) StatusCode.Ok.asRight[StatusCode].pure[F]
    else StatusCode.Unauthorized.asLeft[StatusCode].pure[F]

  def health: F[Either[Unit, Message]] =
    Message(1, "SERVER", "OK")
      .asRight[Unit]
      .pure[F]

  def send(msg: Message): F[Either[Unit, StatusCode]] = {
    logger.info(s"Message: $msg")
    session :+= msg
    session = session.sortWith(_.timestamp < _.timestamp)
    StatusCode.Ok.asRight[Unit].pure[F]
  }

  def sync(s: Sync): F[Either[Unit, Session]] = {
    logger.info(s"Sync request: $s")
    val messagesSinceSync = session.filter(_.timestamp > s.timestamp)
    Session(messagesSinceSync).asRight[Unit].pure[F]
  }

}
