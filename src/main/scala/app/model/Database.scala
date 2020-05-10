package app.model
import doobie.free.connection.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.util.fragment.Fragment
import enumeratum.{Enum, EnumEntry}

sealed trait Table extends EnumEntry {
  def sql: Fragment
  def create: ConnectionIO[Int] = sql.update.run
}

object DatabaseTables extends Enum[Table] {

  def values: IndexedSeq[Table] = findValues

  case object UsersTable extends Table {

    override def sql: Fragment =
      sql"""
           |CREATE TABLE IF NOT EXISTS users (
           |id SERIAL PRIMARY KEY,
           |name VARCHAR(30) NOT NULL,
           |password VARCHAR(30) NOT NULL
           |);
           |""".stripMargin
  }

  case object SessionsTable extends Table {

    override def sql: Fragment =
      sql"""
           |CREATE TABLE IF NOT EXISTS sessions (
           |id UUID PRIMARY KEY,
           |body json NOT NULL
           |);
           |""".stripMargin

  }

  case object ConversationsTable extends Table {

    override def sql: Fragment =
      sql"""
           |CREATE TABLE IF NOT EXISTS conversations (
           |id UUID PRIMARY KEY,
           |name VARCHAR(30) NOT NULL
           |);
           |""".stripMargin
  }

  case object ConversationsAdmins extends Table {

    override def sql: Fragment =
      sql"""
           |CREATE TABLE IF NOT EXISTS conversationsAdmins (
           |id UUID PRIMARY KEY,
           |conv_id UUID REFERENCES conversations(id),
           |user_id INTEGER REFERENCES users(id)
           |);
           |""".stripMargin
  }

  case object ConversationsModerators extends Table {

    override def sql: Fragment =
      sql"""
           |CREATE TABLE IF NOT EXISTS conversationsModerators (
           |id UUID PRIMARY KEY,
           |conv_id UUID REFERENCES conversations(id),
           |user_id INTEGER REFERENCES users(id)
           |);
           |""".stripMargin
  }

  case object ConversationsUsers extends Table {

    override def sql: Fragment =
      sql"""
           |CREATE TABLE IF NOT EXISTS conversationsUsers (
           |id UUID PRIMARY KEY,
           |conv_id UUID REFERENCES conversations(id),
           |user_id INTEGER REFERENCES users(id)
           |);
           |""".stripMargin
  }

}
