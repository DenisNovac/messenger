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
import cats.effect.IO
import cats.syntax.applicativeError._
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.flatMap._
import doobie.implicits._
import app.model.quillmappings.QuillCookieValueWithMetaMapping._
import app.model.quillmappings.QuillInstantMapping._
import com.typesafe.scalalogging.LazyLogging

object PostgresService extends LazyLogging {

  //implicit val lh: LogHandler = LogHandler.jdkLogHandler

  import app.init.Init.postgres.quillContext._
  import app.init.Init.postgres.transactor

  def getUserById(id: Long): IO[Option[MessengerUser]] =
    run(query[MessengerUser].filter(_.id == lift(id)).take(1)).transact(transactor).map(_.headOption)

  def checkUserPassword(id: Long, pwd: String): IO[Option[MessengerUser]] =
    run {
      query[MessengerUser]
        .filter(_.id == lift(id))
        .filter(_.password == lift(pwd))
        .take(1)
    }.transact(transactor)
      .map(_.headOption)

  /** Get user by mutable login (which can be changed but can not be used twice in server) */
  def getUserByEmail(id: Long): Option[MessengerUser] = ???

  def getUserConversations(as: AuthorizedSession): IO[List[Conversation]] = {

    for {
      ids <- run(
              query[ConversationParticipant].filter(_.userId == lift(as.userId))
            ).map(_.map(_.convId))

      convs <- run(
                query[Conversation].filter(cv => liftQuery(ids).contains(cv.id))
              )

    } yield convs
  }.transact(transactor)

  def getConversationsWithMeta(as: AuthorizedSession): IO[Conversations] = {

    val convsWithMeta = PostgresService.getUserConversations(as).flatMap { conversations =>
      conversations
        .traverse(PostgresService.getConversationWithParticipants)
        .map(_.map(PostgresService.convNewToConvWithMeta))
    }
    convsWithMeta.map(l => Conversations(l.toVector))

  }

  def putCookie(cookie: AuthorizedSession): IO[Unit] =
    run(
      query[AuthorizedSession].insert(lift(cookie))
    ).transact(transactor) >> IO.unit

  def getCookie(id: String): IO[AuthorizedSession] =
    run(
      query[AuthorizedSession].filter(_.id == lift(UUID.fromString(id))).take(1)
    ).transact(transactor).map(_.head)

  def addParticipants(convId: UUID, participant: Long): IO[Unit] = {
    for {
      _ <- run(
            query[ConversationParticipant]
              .insert(lift(ConversationParticipant(UUID.randomUUID, convId, participant, 0)))
          )
    } yield ()
  }.transact(transactor)

  def createConversation(newConv: ConversationLegacy): Unit = ???

  def removeConversation(id: Long): Unit = ???

  /** METHODS FOR CONVERSATIONS META CREATION */

  private def getConversationWithParticipants(cv: Conversation): IO[ConversationAppNew] =
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
