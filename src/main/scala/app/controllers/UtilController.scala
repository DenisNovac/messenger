package app.controllers

import sttp.model.StatusCode
import sttp.tapir.{endpoint, statusCode, Endpoint}

object UtilController {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")    // Path
      .out(statusCode) // Output type
      .tag("Util")

}
