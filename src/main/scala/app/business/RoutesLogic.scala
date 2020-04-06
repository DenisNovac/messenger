package app.business

import cats.Monad
import cats.syntax.either._

import cats.syntax.applicative._
import app.model._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.{CookieValueWithMeta, StatusCode}

/**
  * Logic is separate from routes definitions
  * It may be IO (http4s) or Future (Akka Http)
  */
class RoutesLogic[F[_]: Monad] extends LazyLogging {

  var session: Vector[Message] = Vector()

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

  /** Authentication method gives cookie with "secret" for some time */
  def signIn: F[Either[StatusCode, CookieValueWithMeta]] =
    AuthorizationSystem.issueCookie.asRight[StatusCode].pure[F]

  /** Tests cookie */
  def testAuth(cookie: Option[String]): F[Either[StatusCode, StatusCode]] =
    if (AuthorizationSystem.isCookieValid(cookie)) StatusCode.Ok.asRight[StatusCode].pure[F]
    else StatusCode.Forbidden.asLeft[StatusCode].pure[F]
}
