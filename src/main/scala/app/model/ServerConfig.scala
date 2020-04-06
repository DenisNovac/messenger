package app.model

import scala.concurrent.duration.FiniteDuration

case class ServerConfig(
    host: String,
    port: Int,
    server: String,
    sessionTimeout: FiniteDuration
)
