package app.model

import cats.Show
import cats.data.NonEmptyList
import doobie.util.{Get, Put}
import io.circe.Json
import org.postgresql.util.PGobject
import cats.syntax.either._
import cats.syntax.show._
import io.circe.parser._

/**
  * Mapping object to put json objects into PostgreSQL
  * https://tpolecat.github.io/doobie/docs/12-Custom-Mappings.html#defining-get-and-put-for-exotic-types
  * */
object PostgresJsonMapping {
  implicit val showPGobject: Show[PGobject] = Show.show(_.getValue.take(250))

  implicit val jsonGet: Get[Json] =
    Get.Advanced.other[PGobject](NonEmptyList.of("json")).temap[Json] { o =>
      parse(o.getValue).leftMap(_.show)
    }

  implicit val jsonPut: Put[Json] =
    Put.Advanced.other[PGobject](NonEmptyList.of("json")).tcontramap[Json] { j =>
      val o = new PGobject
      o.setType("json")
      o.setValue(j.noSpaces)
      o
    }
}
