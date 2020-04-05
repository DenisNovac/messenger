package app.impl

import app.business.{RoutesDescription, RoutesLogic}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route, RouteResult}

import scala.concurrent.Future
import cats.instances.future._
import sttp.tapir.server.akkahttp._
import sttp.tapir.swagger.akkahttp.SwaggerAkka

import scala.io.StdIn

object AkkaHttpServer extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  val logic = new RoutesLogic[Future]

  /** Routes Tapir to Akka Http */
  val health: Route = RoutesDescription.health.toRoute(_ => logic.health)
  val hello: Route  = RoutesDescription.hello.toRoute(name => logic.hello(name))
  val test: Route   = RoutesDescription.test.toRoute(_ => logic.test)

  val openApiRoute: RequestContext => Future[RouteResult] = new SwaggerAkka(RoutesDescription.openApiYml, "api").routes

  val routes: Route = health ~ hello ~ test ~ openApiRoute

  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Press RETURN to stop...")
  StdIn.readLine() // let it run until user presses return

  bindingFuture
    .flatMap(_.unbind())                 // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
