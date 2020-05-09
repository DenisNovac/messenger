package app.model

import java.time.Instant

import sttp.model.CookieValueWithMeta

trait AuthenticationData

final case class User(id: Long, name: String, password: String) extends AuthenticationData {
  def prettyName: String = s"$name#$id" // name like Sam#111
}

final case class CookieBody(user: User, expires: Option[Instant], body: CookieValueWithMeta) extends AuthenticationData
