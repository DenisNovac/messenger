package app

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/** Для методов jsonFormat */
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

/** Возможность вернуть из роута сообщение */
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

/** Для построения роутов - методы path, get, т.п. */
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/** Модель данных */
sealed trait Data
final case class Message(timestamp: Int, user: String, text: String)
    extends Data
final case class Sync(timestamp: Int) extends Data

object Server extends App {

  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  val logger = Logging(system, "Logger")

  /** Энкодер в JSON */
  implicit val MessageJson: RootJsonFormat[Message] = jsonFormat3(Message)
  implicit val SyncJson: RootJsonFormat[Sync] = jsonFormat1(Sync)


  /** Массив сообщений */
  final case class Session(messages: Vector[Message])
  implicit val SessionJson: RootJsonFormat[Session] = jsonFormat1(Session)

  var session: Vector[Message] = Vector()

  val health: Route =
    path("health") {
      get {
        extractRequestContext { ctx =>
          extractLog { log =>
            complete {
              log.info(s"Health check: ${ctx.request}")
              StatusCodes.OK
            }
          }
        }
      }
    }

  val getTestJsonMessage: Route =
    path("test") {
      get {
        extractRequestContext { ctx =>
          extractLog { log =>
            complete {
              log.info(s"Test message: ${ctx.request}")
              Message(1, "SERVER", "Test message")
            }
          }
        }
      }
    }

  val send: Route =
    path("send") {
      post {
        extractRequestContext { ctx =>
          extractLog { log =>
            entity(as[Message]) { msg =>
              log.info(s"Got message: $msg \n From: $ctx")
              session :+= msg
              session = session.sortWith(_.timestamp < _.timestamp)
              log.info(s"Messages cache: $session")
              complete(StatusCodes.OK)
            }
          }
        }
      }
    }

  val sync: Route =
    path("sync") {
      post {
        extractRequestContext { ctx =>
          extractLog { log =>
            entity(as[Sync]) { sync =>
              log.info(s"Got sync: $sync \n From: $ctx")
              val messagesSinceSync = session.filter(_.timestamp > sync.timestamp)
              complete(Session(messagesSinceSync))
            }
          }
        }
      }
    }





  /** Запуск */
  val host = "127.0.0.1"
  val port = 8080
  val routes: Route = health ~ getTestJsonMessage ~ send ~ sync
  val bindingFuture = Http().bindAndHandle(routes, host, port)
  logger.info(s"Started server on: $host:$port")

  /** Возможность выключить консоль */
  println(s"Press RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  logger.info("Shutting down by user's request...")

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
