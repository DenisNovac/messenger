package app.impl

import app.business.{RoutesDescription, RoutesLogic}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}
import app.model.ServerConfig

import scala.concurrent.Future
import cats.instances.future._
import sttp.tapir.server.akkahttp._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

class AkkaHttpServer(config: ServerConfig) extends ServerImpl(config) {

  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  val logic = new RoutesLogic[Future]

  /** Routes Tapir to Akka Http */
  val health: Route = RoutesDescription.health.toRoute(_ => logic.health)
  val hello: Route  = RoutesDescription.hello.toRoute(name => logic.hello(name))
  val test: Route   = RoutesDescription.test.toRoute(_ => logic.test)

  val openApiRoute: RequestContext => Future[RouteResult] = new SwaggerAkka(RoutesDescription.openApiYml, "api").routes

  val routes: Route = health ~ hello ~ test ~ openApiRoute

  // Server startup
  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, config.host, config.port)

  logger.info(s"Started Akka Http server on ${config.host}:${config.port}")

  override def stop(): Unit =
    bindingFuture
      .flatMap(_.unbind())                 // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
}
