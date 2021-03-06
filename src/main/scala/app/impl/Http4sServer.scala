package app.impl

import app.api.endpoints._
import app.api.controllers._
import app.api.services.AuthService
import app.api.services.db.DatabaseService
import app.model.ServerConfig
import cats.data.Kleisli

import scala.concurrent.ExecutionContext
import cats.syntax.semigroupk._
import cats.effect.{ContextShift, ExitCode, IO, Resource, Timer}
import com.typesafe.scalalogging.LazyLogging
import sttp.tapir.server.http4s._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.syntax.all._
import org.http4s.{HttpRoutes, Request, Response}
import org.http4s.server.{Router, Server}
import sttp.tapir.swagger.http4s.SwaggerHttp4s

class Http4sServer(config: ServerConfig, db: DatabaseService[IO])(implicit val ec: ExecutionContext) {

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)
  implicit val timer: Timer[IO]               = IO.timer(ec)

  /** Services for IO */
  val authService = new AuthService[IO](db, config.sessionTimeout)

  /** Controllers for IO */
  val utilService    = new UtilController[IO]
  val msgService     = new MessagingController[IO](authService, db)
  val authController = new AuthController[IO](authService)

  /** Routes Tapir to Http4s */
  // There is a LOT of errors in IDEA such as Required F Found IO. Application still compiles!
  val health: HttpRoutes[IO]   = UtilEndpoints.health.toRoutes(_ => utilService.health)
  val auth: HttpRoutes[IO]     = AuthEndpoints.signIn.toRoutes(authMsg => authController.signIn(authMsg))
  val authTest: HttpRoutes[IO] = AuthEndpoints.authTest.toRoutes(cookie => authController.testAuth(cookie))

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

  val routes: Kleisli[IO, Request[IO], Response[IO]] = Router("/" -> concat).orNotFound

  val server: Resource[IO, Server[IO]] = BlazeServerBuilder[IO](ec)
    .bindHttp(config.port, config.host)
    .withHttpApp(routes)
    .resource

  /** In Http4s Server there is no need in stop method since server is IO[ExitCode] and IOApp knows how to close it */
  def stop(): Unit = ()
}
