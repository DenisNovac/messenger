package app.impl

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import app.ServerConfigReader
import app.business.routes.{RoutesDescription, RoutesLogic}
import app.model.ServerConfig

import scala.concurrent.Future
import cats.instances.future._
import sttp.tapir.server.akkahttp._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

class AkkaHttpServer extends ServerImpl {

  val config: ServerConfig = ServerConfigReader.config

  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  val logic = new RoutesLogic[Future]

  /** Routes Tapir to Akka Http */
  val health: Route = RoutesDescription.health.toRoute(_ => logic.health)
  val send: Route   = RoutesDescription.send.toRoute(msg => logic.send(msg))
  val sync: Route   = RoutesDescription.sync.toRoute(s => logic.sync(s))

  val openApiRoute: RequestContext => Future[RouteResult] = new SwaggerAkka(RoutesDescription.openApiYml, "api").routes

  val routes: Route = health ~ send ~ sync ~ openApiRoute

  // Server startup
  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.host, config.port)

  logger.info(s"Started Akka Http server on ${config.host}:${config.port}")

  override def stop(): Unit =
    bindingFuture
      .flatMap(_.unbind())                 // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
}
