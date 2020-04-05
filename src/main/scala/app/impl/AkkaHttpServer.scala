package app.impl

import app.generic.{Logic, Routes}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import scala.concurrent.Future
import cats.instances.future._
import sttp.tapir.server.akkahttp._

import scala.io.StdIn

object AkkaHttpServer extends App {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  val logic = new Logic[Future]

  /** Routes Tapir to Akka Http */
  val health: Route = Routes.health.toRoute(_ => logic.health)
  val hello: Route  = Routes.hello.toRoute(name => logic.hello(name))
  val routes: Route = health ~ hello

  val bindingFuture: Future[Http.ServerBinding] = Http().bindAndHandle(routes, "localhost", 8080)

  println(s"Press RETURN to stop...")
  StdIn.readLine() // let it run until user presses return

  bindingFuture
    .flatMap(_.unbind())                 // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
