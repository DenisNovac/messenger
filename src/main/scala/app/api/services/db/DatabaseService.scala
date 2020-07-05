package app.api.services.db

import java.util.UUID

import app.model._
import cats.effect.Async
import com.typesafe.scalalogging.LazyLogging

trait DatabaseService[F[_]] extends LazyLogging {

  /** Get user by immutable ID */
  def getUserById(id: Long): F[Option[MessengerUser]]

  def checkUserPassword(id: Long, pwd: String): F[Option[MessengerUser]]

  def getUserConversations(as: AuthorizedSession): F[List[Conversation]]

  def getConversationsWithMeta(as: AuthorizedSession): F[Conversations]

  def putCookie(cookie: AuthorizedSession): F[Unit]

  def getCookie(id: UUID): F[Option[AuthorizedSession]]

  def addParticipantsToConversation(convId: UUID, participant: Long): F[Unit]

}
