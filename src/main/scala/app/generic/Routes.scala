package app.generic

import sttp.model.StatusCode
import sttp.tapir._

object Routes {

  val health: Endpoint[Unit, Unit, StatusCode, Nothing] =
    endpoint.get
      .in("health")    // Path
      .out(statusCode) // Output type

  // GET /hello?name=...
  val hello: Endpoint[String, Unit, String, Nothing] =
    endpoint.get
      .in("hello")
      .in(query[String]("name")) // Parameter
      .out(stringBody)

}
