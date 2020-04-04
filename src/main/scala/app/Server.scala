package app

import org.log4s.getLogger
import org.http4s.{Http, HttpRoutes, Request, Response}
import org.http4s.server.Router
import org.http4s.server.middleware.Logger
import org.http4s.syntax.all._
import org.http4s.dsl.io._
import cats.syntax.functor._
import cats.data.Kleisli
import cats.effect.{ContextShift, ExitCode, IO, IOApp}

import scala.concurrent.ExecutionContext.global

/** Модель данных */
sealed trait Data
final case class Message(timestamp: Int, user: String, text: String)
    extends Data
final case class Sync(timestamp: Int) extends Data

object Routes {

  val health: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "health" =>
      Ok()
  }

}

object Server extends IOApp {
  private val globalLogger = getLogger("Master")

  implicit val cs: ContextShift[IO] = IO.contextShift(global)

  val host = "127.0.0.1"
  val port = 8080

  import org.http4s.server.blaze.BlazeServerBuilder

  val routes: Kleisli[IO, Request[IO], Response[IO]] = Router(
    "/" -> Routes.health
  ).orNotFound

  val loggedRoutes: Http[IO, IO] = Logger.httpApp(true, true)(routes)

  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(port, host)
      .withHttpApp(loggedRoutes)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
}
