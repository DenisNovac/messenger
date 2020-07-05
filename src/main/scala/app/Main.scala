package app

import app.api.services.db.TransactorDatabaseService
import app.impl.Http4sServer
import app.init.DatabaseSession
import app.model.ServerConfig
import cats.effect.ExitCase.Canceled
import cats.effect.{Blocker, ContextShift, ExitCode, IO, IOApp, Resource}
import com.typesafe.scalalogging.LazyLogging
import org.http4s.server.Server
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._

import scala.concurrent.ExecutionContext

object Main extends IOApp with LazyLogging {

  implicit private val ec: ExecutionContext = ExecutionContext.global
  implicit private val cs: ContextShift[IO] = IO.contextShift(ec)

  override def run(args: List[String]): IO[ExitCode] =
    appResources.use(_ => IO.never).as(ExitCode.Success).guaranteeCase {
      case Canceled => IO(logger.info("Server successfully stopped"))
      case r        => IO(logger.error(s"Server stopped by unknown error: $r"))
    }

  private def appResources: Resource[IO, Server[IO]] =
    for {

      blocker <- Blocker[IO]
      config <- Resource.liftF(
                 CatsEffectConfigSource(ConfigSource.file("./application.conf")).loadF[IO, ServerConfig](blocker)
               )

      _ <- Resource.liftF(IO(logger.info(s"Application config: $config")))

      dbSession = new DatabaseSession(config.db)

      _ <- Resource.liftF(dbSession.runMigrations)

      dbService = new TransactorDatabaseService[IO](dbSession.transactor)

      s <- new Http4sServer(config, dbService).server

      _ <- Resource.liftF(IO(logger.info(s"Server started on ${config.host}:${config.port}")))
    } yield s
}
