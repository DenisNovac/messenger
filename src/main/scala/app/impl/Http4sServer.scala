package app.impl

import app.business.{RoutesDescription, RoutesLogic}
import app.model.ServerConfig

import scala.concurrent.ExecutionContext
import cats.syntax.functor._
import cats.syntax.semigroupk._
import cats.effect.{CancelToken, ContextShift, ExitCode, IO, Timer}
import sttp.tapir.server.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class Http4sServer(config: ServerConfig) extends ServerImpl(config) {

  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]               = IO.timer(ec)

  val logic = new RoutesLogic[IO]

  /** Routes Tapir to Http4s */
  val health: HttpRoutes[IO] = RoutesDescription.health.toRoutes(_ => logic.health)
  val send: HttpRoutes[IO]   = RoutesDescription.send.toRoutes(msg => logic.send(msg))
  val sync: HttpRoutes[IO]   = RoutesDescription.sync.toRoutes(c => logic.sync(c))

  /** Return OpenAPI route with "/api" path */
  val openApiRoute: HttpRoutes[IO] = new SwaggerHttp4s(RoutesDescription.openApiYml, contextPath = "api").routes[IO]

  val concat: HttpRoutes[IO] = health <+> send <+> sync <+> openApiRoute

  val routes = Router("/" -> concat).orNotFound

  val server: IO[ExitCode] = BlazeServerBuilder[IO]
    .bindHttp(config.port, config.host)
    .withHttpApp(routes)
    .serve
    .compile
    .drain
    .as(ExitCode.Success)

  val cancelable: CancelToken[IO] = server.unsafeRunCancelable(r => println(s"Done: $r"))

  logger.info(s"Started Http4s server on ${config.host}:${config.port}")

  override def stop(): Unit =
    cancelable.unsafeRunSync()
}
