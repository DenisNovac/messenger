package app.init

import app.model.DatabaseConfig
import doobie.Transactor
import cats.Monad
import cats.effect.{CancelToken, ContextShift, IO, SyncIO}
import cats.syntax.applicative._
import cats.free.Free
import com.typesafe.scalalogging.LazyLogging
import doobie.free.connection
import doobie.free.connection.ConnectionIO
import doobie.implicits._

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
  private val transactor       = Transactor.fromDriverManager[IO](driver, connectionString, config.user, config.password)

  /** Database initialization will wait for database. For some time... */

  private val createUsers =
    sql"""
         |CREATE TABLE IF NOT EXISTS users (
         |id SERIAL PRIMARY KEY,
         |name VARCHAR(30) NOT NULL,
         |password VARCHAR(30) NOT NULL
         |);
         |""".stripMargin

  private val createSessions =
    sql"""
         |CREATE TABLE IF NOT EXISTS sessions (
         |id UUID PRIMARY KEY,
         |body json NOT NULL
         |);
         |""".stripMargin

  private val initTables = {
    for {
      _ <- createUsers.update.run
      _ <- createSessions.update.run
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
