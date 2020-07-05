package app.api.services.db
import java.util.UUID

import app.model.{
  AuthorizedSession,
  Conversation,
  ConversationAppNew,
  ConversationBody,
  ConversationLegacy,
  ConversationParticipant,
  Conversations,
  MessengerUser
}
import cats.effect.{Async, Bracket, Concurrent, IO, Sync}
import doobie.implicits._
import app.model.quillmappings.QuillCookieValueWithMetaMapping._
import app.model.quillmappings.QuillInstantMapping._
import app.model.quillmappings.QuillPostgresContext
import com.typesafe.scalalogging.LazyLogging
import app.model.quillmappings.QuillPostgresContext.ctx._
import cats.{Applicative, Monad}
import doobie.util.transactor.Transactor
import cats.implicits._

class TransactorDatabaseService[F[_]: Async](transactor: Transactor[F]) extends DatabaseService[F] with LazyLogging {

  //implicit val lh: LogHandler = LogHandler.jdkLogHandler

  override def getUserById(id: Long): F[Option[MessengerUser]] =
    run(query[MessengerUser].filter(_.id == lift(id)).take(1)).transact(transactor).map(_.headOption)

  override def checkUserPassword(id: Long, pwd: String): F[Option[MessengerUser]] =
    run {
      query[MessengerUser]
        .filter(_.id == lift(id))
        .filter(_.password == lift(pwd))
        .take(1)
    }.transact(transactor)
      .map(_.headOption)

  override def getUserConversations(as: AuthorizedSession): F[List[Conversation]] = {

    for {
      ids <- run(
              query[ConversationParticipant].filter(_.userId == lift(as.userId))
            ).map(_.map(_.convId))

      convs <- run(
                query[Conversation].filter(cv => liftQuery(ids).contains(cv.id))
              )

    } yield convs
  }.transact(transactor)

  override def getConversationsWithMeta(as: AuthorizedSession): F[Conversations] = {

    val convsWithMeta = getUserConversations(as).flatMap { conversations =>
      conversations
        .traverse(getConversationWithParticipants)
        .map(_.map(convNewToConvWithMeta))
    }
    convsWithMeta.map(l => Conversations(l.toVector))

  }

  override def putCookie(cookie: AuthorizedSession): F[Unit] =
    run(
      query[AuthorizedSession].insert(lift(cookie))
    ).transact(transactor) >> ().pure[F]

  override def getCookie(id: UUID): F[Option[AuthorizedSession]] =
    run(
      query[AuthorizedSession].filter(_.id == lift(id))
    ).transact(transactor).map(_.headOption)

  override def addParticipantsToConversation(convId: UUID, participant: Long): F[Unit] = {
    for {
      _ <- run(
            query[ConversationParticipant]
              .insert(lift(ConversationParticipant(UUID.randomUUID, convId, participant, 0)))
          )
    } yield ()
  }.transact(transactor)

  /** METHODS FOR CONVERSATIONS META CREATION */

  private def getConversationWithParticipants(cv: Conversation): F[ConversationAppNew] =
    for {
      participants <- run(
                       query[ConversationParticipant].filter(_.convId == lift(cv.id))
                     ).transact(transactor)
    } yield ConversationAppNew(cv, participants)

  /** Parse statuses to users roles */
  private def convNewToConvWithMeta(n: ConversationAppNew): ConversationLegacy = {

    val parsed = n.ptc.collect {
      case ConversationParticipant(id, convId, userId, 1) =>
        ConversationLegacy(n.conv.id, ConversationBody(n.conv.name, Set(userId), Set(), Set()))
      case ConversationParticipant(id, convId, userId, 2) =>
        ConversationLegacy(n.conv.id, ConversationBody(n.conv.name, Set(), Set(userId), Set()))
      case ConversationParticipant(id, convId, userId, anyStatus) =>
        ConversationLegacy(n.conv.id, ConversationBody(n.conv.name, Set(), Set(), Set(userId)))
    }

    if (parsed.length > 1) {
      parsed.foldRight(parsed.head.empty)(_ ++ _)
    } else parsed.head

  }
}
