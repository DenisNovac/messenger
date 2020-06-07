package app.model.quillmappings

import doobie.quill.DoobieContext
import io.getquill.NamingStrategy

object QuillPostgresContext {

  /**
    * naming scheme: https://getquill.io/#contexts-sql-contexts-naming-strategy
    * must be like MessengerUser -> messenger_user
    */
  val ctx = new DoobieContext.Postgres(NamingStrategy(io.getquill.SnakeCase, io.getquill.LowerCase))
}
