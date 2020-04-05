package app


import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.server.ServerDefaults.StatusCodes

/** Generic logic */
import cats.Monad  // for generic logic on pure syntax
import cats.syntax.either._ // for asRight
import cats.syntax.applicative._  // for pure


/** For http4s server */
import scala.concurrent.ExecutionContext
import cats.syntax.functor._ // for as
import cats.syntax.semigroupk._ // for <+>
import cats.effect.{IO, ContextShift, Timer, ExitCode}

import sttp.tapir.server.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._ // for orNotFound
import org.http4s.HttpRoutes
import org.http4s.server.Router


sealed trait Data
final case class Message(timestamp: Int, user: String, text: String)
    extends Data
final case class Sync(timestamp: Int) extends Data

/** Routes separately from logic */
object Routes {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health") // Path
      .out(statusCode) // Output type

  // GET /hello?name=...
  val hello: Endpoint[String, Unit, String, Nothing] =
    endpoint.get
      .in("hello")
      .in(query[String]("name")) // Parameter
      .out(stringBody)

}

/**
  * Logic is separate from routes definitions
  * It may be IO (http4s) or Future (Akka Http)
  */
class Logic[F[_]: Monad] {

  def health: F[Either[Unit, StatusCode]] =
    StatusCodes.success
      .asRight[Unit]
      .pure[F]

  def hello(name: String): F[Either[Unit, String]] =
    s"Hello, $name".asRight[Unit].pure[F]
}

object Http4sServer extends App {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO] = IO.timer(ec)

  // import cats.instances.future._ if one want to get Logic from Future
  val logic = new Logic[IO]

  /** Routes Tapir to Http4s */
  val health: HttpRoutes[IO] = Routes.health.toRoutes(_ => logic.health)
  val hello: HttpRoutes[IO] = Routes.hello.toRoutes(name => logic.hello(name))
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
