package app.init

import java.util.UUID

import app.model.{ConversationBody, DatabaseConfig, MessengerUser}
import cats.effect.{CancelToken, ContextShift, IO}
import cats.effect.implicits._
import cats.instances.list._
import cats.syntax.flatMap._
import cats.syntax.applicative._
import cats.syntax.traverse._
import com.typesafe.scalalogging.LazyLogging
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.quill.DoobieContext
import doobie.util.update.Update
import io.getquill.NamingStrategy
import javax.sql.DataSource
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

import scala.concurrent.ExecutionContext

/**
  * Object which contains database connection for shared access and creates initial tables and users
  * Only IO-monad
  */
class PostgresSession(config: DatabaseConfig)(implicit val ec: ExecutionContext) extends LazyLogging {

  logger.info(s"Database config: $config")

  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  // naming scheme: https://getquill.io/#contexts-sql-contexts-naming-strategy
  // must be like MessengerUser -> messenger_user
  val quillContext = new DoobieContext.Postgres(NamingStrategy(io.getquill.SnakeCase, io.getquill.LowerCase))
  import quillContext._

  private val driver           = "org.postgresql.Driver"
  private val connectionString = s"jdbc:postgresql://${config.host}:${config.port}/${config.name}"

  val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](driver, connectionString, config.user, config.password)

  private val usersList = List(
    MessengerUser(1, "denis", "123"),
    MessengerUser(2, "filya", "123"),
    MessengerUser(3, "ivan", "123")
  )

  private val convsList = List(
    ConversationBody("test", Set(1), Set(), Set(2, 3)),
    ConversationBody("test2", Set(3), Set(), Set(1, 2)),
    ConversationBody("test3", Set(2), Set(), Set(1))
  )

  /** Return a List of ConnectionIO. It should be transformed to ConnectionIO[List] to execute */
  def insertConversation(conversation: ConversationBody): List[doobie.ConnectionIO[Int]] = {

    def insertConversationRelation(convId: UUID, users: Set[Long], status: Int): doobie.ConnectionIO[Int] = {
      val sql       = s"INSERT INTO conversation_participant(id, conv_id, user_id, status) VALUES (?, ?, ?, ?)"
      val withUuids = users.map(user => (UUID.randomUUID, convId, user, status)).toList
      Update[(UUID, UUID, Long, Int)](sql).updateMany(withUuids)
    }

    val uuid = UUID.randomUUID

    List(
      sql"""
           |INSERT INTO conversation(id, name) VALUES ($uuid, ${conversation.name})
           |""".stripMargin.update.run,
      insertConversationRelation(uuid, conversation.admins, 0),
      insertConversationRelation(uuid, conversation.mods, 2),
      insertConversationRelation(uuid, conversation.participants, 1)
    )

  }

  private val initConversations = {
    for {
      c <- convsList
    } yield insertConversation(c)
  }.flatten.sequence

  /**
    * Make Liquibase migration on PostgreSQL through a Doobie transactor
    */
  private def migrate: IO[Either[Throwable, Unit]] = {
    logger.info("Migrations module started")
    import doobie.free.FC // raw db connection alias

    FC.raw { conn =>
        val resourceAccessor = new ClassLoaderResourceAccessor(getClass.getClassLoader)
        val database         = DatabaseFactory.getInstance.findCorrectDatabaseImplementation(new JdbcConnection(conn))
        val liquibase        = new Liquibase(config.migrations, resourceAccessor, database)
        liquibase.update("")
      }
      .transact(transactor)
      .attempt
  }

  private val initTables: IO[Either[Throwable, Unit]] = {
    for {
      _ <- usersList.traverse(u => run(query[MessengerUser].insert(lift(u))))
      _ <- initConversations
    } yield ()
  }.transact(transactor).attempt

  /** Database initialization will wait for database some time */

  private def withRetry(retries: Int, waitSecs: Int, target: IO[Either[Throwable, Unit]]): IO[Unit] = target.flatMap {
    case Left(error) if retries > 0 =>
      logger.error(
        s"Database error: ${error.getMessage}. Will retry in ${waitSecs} seconds. Retries left: ${retries - 1}"
      )
      Thread.sleep(waitSecs * 1000)
      withRetry(retries - 1, waitSecs, target)
    case Left(error) =>
      logger.error(s"Database error: ${error.getMessage}, stopping")
      throw error
    case _ =>
      logger.info(s"Database was initialized")
      IO.unit
  }

  private val cancelableInit: CancelToken[IO] =
    (withRetry(3, 20, migrate) >>
      withRetry(3, 20, initTables)).unsafeRunCancelable(r => IO())

  /** Method to cancel initialization which was not completed yet */
  def cancelInit(): Unit = {
    logger.info("Aborting database initialization")
    cancelableInit.unsafeRunSync()
  }
}
