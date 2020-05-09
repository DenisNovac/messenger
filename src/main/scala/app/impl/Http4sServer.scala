package app.impl

import app.api.endpoints._
import app.api.controllers._
import app.init.Init
import app.model.ServerConfig

import scala.concurrent.{ExecutionContext, Future}
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

  val config: ServerConfig = Init.config

  implicit val ec: ExecutionContext           = scala.concurrent.ExecutionContext.global
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]               = IO.timer(ec)

  /** Services for IO */
  val utilService = new UtilController[IO]
  val msgService  = new MessagingController[IO]
  val authService = new AuthController[IO]

  /** Routes Tapir to Http4s */
  // There is a LOT of errors in IDEA such as Required F Found IO. Application still compiles!
  val health: HttpRoutes[IO]   = UtilEndpoints.health.toRoutes(_ => utilService.health)
  val auth: HttpRoutes[IO]     = AuthEndpoints.signIn.toRoutes(authMsg => authService.signIn(authMsg))
  val authTest: HttpRoutes[IO] = AuthEndpoints.authTest.toRoutes(cookie => authService.testAuth(cookie))

  val send: HttpRoutes[IO] = MessagingEndpoints.send.toRoutes(l => msgService.send(l._1, l._2))
  val sync: HttpRoutes[IO] = MessagingEndpoints.sync.toRoutes(l => msgService.sync(l._1, l._2))

  val addToConversation: HttpRoutes[IO] =
    MessagingEndpoints.addToConversation.toRoutes(l => msgService.addToConversation(l._1, l._2))

  val conversations: HttpRoutes[IO] =
    MessagingEndpoints.conversations.toRoutes(cookie => msgService.conversationsList(cookie))

  /** Return OpenAPI route with "/api" path */
  val openApiRoute: HttpRoutes[IO] = new SwaggerHttp4s(OpenApiEndpoint.openApiYml, contextPath = "api").routes[IO]

  val concat
      : HttpRoutes[IO] = health <+> send <+> sync <+> auth <+> authTest <+> conversations <+> addToConversation <+> openApiRoute

  val routes = Router("/" -> concat).orNotFound

  val server: IO[ExitCode] = BlazeServerBuilder[IO](ec)
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
