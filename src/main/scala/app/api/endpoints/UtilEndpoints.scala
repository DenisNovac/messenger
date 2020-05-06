package app.api.endpoints

import sttp.model.StatusCode
import sttp.tapir.{endpoint, statusCode, Endpoint}

object UtilEndpoints {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")    // Path
      .out(statusCode) // Output type
      .tag("Util")

}
