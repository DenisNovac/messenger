package app.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import app.api.endpoints.OpenApiEndpoint
import app.model.ServerConfig
import app.api.controllers._
import app.init.Init
import cats.effect.{ContextShift, ExitCode, IO, Timer}

import scala.concurrent.{ExecutionContext, Future}
import cats.instances.future._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

/** Deprecated for now, no development */
class AkkaHttpServer(implicit val ec: ExecutionContext) extends ServerImpl {

  val config: ServerConfig = Init.config

  implicit val system: ActorSystem = ActorSystem()

  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  val utilService = new UtilController[Future]
  val msgService  = new MessagingController[Future]
  val authService = new AuthController[Future]

  /** Routes Tapir to Akka Http */
  /*val health: Route = RoutesDescription.health.toRoute(_ => logic.health)
  val send: Route   = RoutesDescription.send.toRoute(msg => logic.send(msg))
  val sync: Route   = RoutesDescription.sync.toRoute(s => logic.sync(s))*/

  val openApiRoute: RequestContext => Future[RouteResult] = new SwaggerAkka(OpenApiEndpoint.openApiYml, "api").routes

  val routes: Route = /*health ~ send ~ sync ~ */ openApiRoute

  // Server startup
  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.host, config.port)

  /** This is an trick to make cancelable IOApp with Akka's Future */
  override val server: IO[ExitCode] = IO.never.map(_ => ExitCode.Success)

  logger.info(s"Started Akka Http server on ${config.host}:${config.port}")

  override def stop(): Unit =
    bindingFuture
      .flatMap(_.unbind())                 // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
}
