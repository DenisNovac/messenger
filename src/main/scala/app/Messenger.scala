package app

import app.api.services.db.{DatabaseService, TransactorDatabaseService}
import app.impl.Http4sServer
import app.init.DatabaseSession
import app.model.ServerConfig
import cats.effect.ExitCase.Canceled
import cats.effect.{Blocker, ExitCase, ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.LazyLogging
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

import scala.concurrent.ExecutionContext

object Messenger extends IOApp with LazyLogging {

  implicit private val ec: ExecutionContext = ExecutionContext.global

  override def run(args: List[String]): IO[ExitCode] = app.allocated.map(_._1).guaranteeCase {
    case Canceled =>
      IO {
        logger.info("Server was stopped by cancelling")
      }

    case r =>
      IO {
        logger.error(s"Server was stopped by unknown reason: $r")
      }
  }

  private def app =
    for {

      blocker <- Blocker[IO]
      config <- Resource.liftF(
                 CatsEffectConfigSource(ConfigSource.file("./application.conf")).loadF[IO, ServerConfig](blocker)
               )

      _ <- Resource.liftF(IO(logger.info(s"Application config: $config")))

      dbSession = new DatabaseSession(config.db)

      _ <- Resource.liftF(dbSession.runMigrations)

      dbService = new TransactorDatabaseService[IO](dbSession.transactor)

      _ <- new Http4sServer(config, dbService).server

    } yield ExitCode.Success
}
