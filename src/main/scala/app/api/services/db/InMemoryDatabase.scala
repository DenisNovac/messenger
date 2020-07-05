package app.api.services.db

import app.model._
import com.typesafe.scalalogging.LazyLogging

/** In-memory structure for chat */
object InMemoryDatabase {

  private var messages: Vector[NormalizedTextMessage] = Vector.empty

  def putMessage(msg: NormalizedTextMessage): Unit =
    messages :+= msg

  def getMessages: Vector[NormalizedTextMessage] = {
    val v = messages
    v
  }

}
