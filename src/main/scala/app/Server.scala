package app

import cats.effect.ExitCode
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerDefaults.StatusCodes

/** Секция построения реализации */
import scala.concurrent.ExecutionContext

import cats.syntax.either._ // для asRight
import cats.syntax.functor._ // для as
import cats.syntax.semigroupk._  // для <+>
import cats.effect.IO
import cats.effect.ContextShift
import cats.effect.Timer

import sttp.tapir.server.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._ // для orNotFound
import org.http4s.HttpRoutes
import org.http4s.server.Router

/** Модель данных */
sealed trait Data
final case class Message(timestamp: Int, user: String, text: String)
    extends Data
final case class Sync(timestamp: Int) extends Data

object Routes {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")
      .out(statusCode)

  // GET /hello?name=...
  val hello: Endpoint[String, Unit, String, Nothing] =
    endpoint.get
      .in("hello")
      .in(query[String]("name"))
      .out(stringBody)

}

object Server extends App {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  /** Преобразование роутов Tapir в Http4s */
  val health: HttpRoutes[IO] = Routes.health.toRoutes(_ => IO(StatusCodes.success.asRight[Unit]))
  val hello: HttpRoutes[IO] = Routes.hello.toRoutes(name => IO(s"Hello, $name".asRight[Unit]))
  val concat = health <+> hello

  val routes = Router("/" -> concat).orNotFound

  val server: IO[ExitCode] = BlazeServerBuilder[IO]
    .bindHttp(8080, "localhost")
    .withHttpApp(routes)
    .serve
    .compile
    .drain
    .as(ExitCode.Success)

  server.unsafeRunSync()

}
