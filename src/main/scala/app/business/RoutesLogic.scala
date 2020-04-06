package app.business

import java.time.Instant

import cats.Monad
import cats.syntax.either._
import cats.syntax.option._
import cats.syntax.applicative._
import app.model._
import com.typesafe.scalalogging.LazyLogging
import sttp.model.{CookieValueWithMeta, StatusCode}

/**
  * Logic is separate from routes definitions
  * It may be IO (http4s) or Future (Akka Http)
  */
class RoutesLogic[F[_]: Monad] extends LazyLogging {

  val cookieTimeoutSecs = 10

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
  def getAuth(): F[Either[StatusCode, CookieValueWithMeta]] = {
    logger.info("Giving an cookie")
    val e: Option[Instant] = Instant.ofEpochMilli(Instant.now().toEpochMilli + cookieTimeoutSecs * 1000L).some

    CookieValueWithMeta(value = "secret", expires = e, None, None, None, false, false, Map())
      .asRight[StatusCode]
      .pure[F]
  }

  /** Tests cookie */
  def authTest(cookie: Option[String]) = {
    logger.info(s"Cookie: $cookie")
    if (cookie.getOrElse("") == "secret")
      StatusCode.Ok.asRight[StatusCode].pure[F]
    else
      StatusCode.Forbidden.asLeft[StatusCode].pure[F]
  }
}
