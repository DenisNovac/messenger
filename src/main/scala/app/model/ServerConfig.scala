package app.model

import scala.concurrent.duration.FiniteDuration

final case class ServerConfig(
    host: String,
    port: Int,
    server: String,
    sessionTimeout: FiniteDuration,
    db: DatabaseConfig
)

final case class DatabaseConfig(
    host: String,
    port: Int,
    name: String,
    user: String,
    password: String
)
