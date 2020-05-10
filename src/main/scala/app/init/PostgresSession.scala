package app.init

import java.util.UUID

import app.model.{Conversation, ConversationBody, DatabaseConfig, DatabaseTables, User}
import doobie.Transactor
import cats.Monad
import cats.effect.{CancelToken, ContextShift, IO, SyncIO}
import cats.syntax.applicative._
import cats.syntax.traverse._
import cats.instances.list._
import cats.free.Free
import com.typesafe.scalalogging.LazyLogging
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.postgres.implicits._
import doobie.util.log.LogHandler
import doobie.util.transactor.Transactor.Aux
import doobie.util.update.Update

import scala.concurrent.ExecutionContext

/**
  * Object which contains database connection for shared access
  * Only IO-monad
  */
class PostgresSession(config: DatabaseConfig)(implicit val ec: ExecutionContext) extends LazyLogging {

  logger.info(s"Database config: $config")

  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  private val driver           = "org.postgresql.Driver"
  private val connectionString = s"jdbc:postgresql://${config.host}:${config.port}/${config.name}"

  val transactor: Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](driver, connectionString, config.user, config.password)

  /** Database initialization will wait for database. For some time... */

  private val usersList = List(
    User(1, "denis", "123"),
    User(2, "filya", "123"),
    User(3, "ivan", "123")
  )

  private val convsList = List(
    ConversationBody("test", Set(1), Set(), Set(1, 2, 3)),
    ConversationBody("test2", Set(3), Set(), Set(1, 2, 3)),
    ConversationBody("test3", Set(2), Set(), Set(1, 2))
  )

  def selectUserById(id: Long) =
    sql"""
         |SELECT * FROM users WHERE id = $id;
         |""".stripMargin.query[User].to[List].transact(transactor)

  def insertUser(user: User) =
    sql"""
         |INSERT INTO users(id, name, password) VALUES (${user.id}, ${user.name}, ${user.password});
         |""".stripMargin

  /** Return a List of ConnectionIO. It should be transformed to ConnectionIO[List] to execute */
  def insertConversation(conversation: ConversationBody): List[doobie.ConnectionIO[Int]] = {

    def insertConversationRelation(relationName: String, convId: UUID, users: Set[Long]): doobie.ConnectionIO[Int] = {
      val sql       = s"INSERT INTO $relationName(id, conv_id, user_id) VALUES (?, ?, ?)"
      val withUuids = users.map(user => (UUID.randomUUID, convId, user)).toList
      Update[(UUID, UUID, Long)](sql).updateMany(withUuids)
    }

    val uuid = UUID.randomUUID

    List(
      sql"""
           |INSERT INTO conversations(id, name) VALUES ($uuid, ${conversation.name})
           |""".stripMargin.update.run,
      insertConversationRelation("conversationsAdmins", uuid, conversation.admins),
      insertConversationRelation("conversationsModerators", uuid, conversation.mods),
      insertConversationRelation("conversationsUsers", uuid, conversation.participants)
    )

  }

  private val initUsers = {
    for {
      u <- usersList
    } yield insertUser(u)
  }.foldRight(Fragment.empty)((fr1, fr2) => fr1 ++ fr2)

  private val initConversations = {
    for {
      c <- convsList
    } yield insertConversation(c)
  }.flatten.sequence

  private val initTables = {
    for {
      _ <- DatabaseTables.values.map(_.create).toList.sequence
      _ <- initUsers.update.run
      _ <- initConversations
    } yield ()
  }.transact(transactor).attempt

  private def initTablesWithRetry(retries: Int, waitSecs: Int): IO[Unit] = initTables.flatMap {
    case Left(error) if retries > 0 =>
      logger.error(
        s"Database error: ${error.getMessage}. Will retry in ${waitSecs} seconds. Retries left: ${retries - 1}"
      )
      Thread.sleep(waitSecs * 1000)
      initTablesWithRetry(retries - 1, waitSecs)
    case Left(error) =>
      logger.error(s"Database error: ${error.getMessage}, stopping")
      throw error
    case Right(value) =>
      logger.info(s"Database was initialized")
      value.pure[IO]
  }

  private val cancelableInit: CancelToken[IO] = initTablesWithRetry(3, 20).unsafeRunCancelable(r => IO())

  /** Method to cancel initialization which was not completed yet */
  def cancelInit(): Unit = {
    logger.info("Aborting database initialization")
    cancelableInit.unsafeRunSync()
  }
}
