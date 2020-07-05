package app.init

import app.model.DatabaseConfig
import cats.effect.{CancelToken, ContextShift, IO}
import com.typesafe.scalalogging.LazyLogging
import doobie.Transactor
import doobie.implicits._
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

import scala.concurrent.ExecutionContext

/**
  * Object which contains database connection for shared access and runs Liquibase migrations
  */
class DatabaseSession(config: DatabaseConfig)(implicit val ec: ExecutionContext) extends LazyLogging {

  logger.info(s"Database config: $config")

  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  private val driver           = config.driver
  private val connectionString = s"${config.url}/${config.database}"

  val transactor: Transactor[IO] =
    Transactor.fromDriverManager[IO](driver, connectionString, config.user, config.password)

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
      logger.info(s"Database migration is done")
      IO.unit
  }

  def runMigrations: IO[Unit] =
    withRetry(3, 20, migrate)

}
