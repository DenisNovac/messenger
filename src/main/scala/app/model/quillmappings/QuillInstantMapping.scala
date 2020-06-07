package app.model.quillmappings

import java.time.Instant
import java.util.Date

import io.getquill.MappedEncoding

object QuillInstantMapping {
  implicit val instantEncoding: MappedEncoding[Instant, Date] = MappedEncoding[Instant, Date](Date.from)
  implicit val instantDecoding: MappedEncoding[Date, Instant] = MappedEncoding[Date, Instant](_.toInstant())
}
