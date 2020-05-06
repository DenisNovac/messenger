package app.impl

import app.ServerConfigReader
import app.business.routes.RoutesLogic
import app.controllers._
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

class Http4sServer extends ServerImpl {

  val config: ServerConfig = ServerConfigReader.config

  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]               = IO.timer(ec)

  val logic = new RoutesLogic[IO]

  /** Routes Tapir to Http4s */
  // There is a LOT of errors in IDEA such as Required F Found IO. Application still compiles!
  val health: HttpRoutes[IO]   = UtilController.health.toRoutes(_ => logic.health)
  val auth: HttpRoutes[IO]     = AuthController.signIn.toRoutes(authMsg => logic.signIn(authMsg))
  val authTest: HttpRoutes[IO] = AuthController.authTest.toRoutes(cookie => logic.testAuth(cookie))

  val send: HttpRoutes[IO] = MessagingController.send.toRoutes(l => logic.send(l._1, l._2))
  val sync: HttpRoutes[IO] = MessagingController.sync.toRoutes(l => logic.sync(l._1, l._2))

  val addToConversation: HttpRoutes[IO] =
    MessagingController.addToConversation.toRoutes(l => logic.addToConversation(l._1, l._2))

  val conversations: HttpRoutes[IO] =
    MessagingController.conversations.toRoutes(cookie => logic.conversationsList(cookie))

  /** Return OpenAPI route with "/api" path */
  val openApiRoute: HttpRoutes[IO] = new SwaggerHttp4s(OpenApiController.openApiYml, contextPath = "api").routes[IO]

  val concat
      : HttpRoutes[IO] = health <+> send <+> sync <+> auth <+> authTest <+> conversations <+> addToConversation <+> openApiRoute

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
