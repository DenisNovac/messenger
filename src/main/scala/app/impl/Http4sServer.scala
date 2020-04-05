package app.impl

import app.generic.{Logic, Routes}

import scala.concurrent.ExecutionContext
import cats.syntax.functor._
import cats.syntax.semigroupk._
import cats.effect.{ContextShift, ExitCode, IO, Timer}
import sttp.tapir.server.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.tapir.swagger.http4s.SwaggerHttp4s

object Http4sServer extends App {
  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]               = IO.timer(ec)

  val logic = new Logic[IO]

  /** Routes Tapir to Http4s */
  val health: HttpRoutes[IO] = Routes.health.toRoutes(_ => logic.health)
  val hello: HttpRoutes[IO]  = Routes.hello.toRoutes(name => logic.hello(name))

  /** Return OpenAPI route with "/api" path */
  val openApiRoute: HttpRoutes[IO] = new SwaggerHttp4s(Routes.openApiYml, contextPath = "api").routes[IO]

  val concat: HttpRoutes[IO] = health <+> hello <+> openApiRoute

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
