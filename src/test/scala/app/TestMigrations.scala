package app

import java.io.File

import app.api.services.AuthService
import app.init.DatabaseSession
import app.model.{Authorize, ConversationBody, DatabaseConfig, MessengerUser}
import doobie.quill.DoobieContext
import io.getquill.NamingStrategy
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import doobie.implicits._

import scala.concurrent.ExecutionContext
import scala.reflect.io.Directory

class TestMigrations extends AnyFunSuite with BeforeAndAfterAll {

  implicit private val ec = ExecutionContext.global

  private val h2Ctx = new DoobieContext.H2(NamingStrategy(io.getquill.SnakeCase, io.getquill.LowerCase))
  import h2Ctx._

  private val config = DatabaseConfig(
    driver = "org.h2.Driver",
    url = "jdbc:h2:./h2TestDir",
    database = "h2test",
    user = "",
    password = "",
    migrations = "liquibase/changelog.xml"
  )

  private val testUser0 = MessengerUser(0, "Tester", "test")
  private val testUser1 = MessengerUser(1, "Not admin", "test")

  test("Test migrations") {
    val db         = new DatabaseSession(config)
    val transactor = db.transactor

    // run migrations synchronically and wait for them to end
    db.runMigrations.unsafeRunSync

    val insert =
      h2Ctx
        .run {
          liftQuery(List(testUser0, testUser1)).foreach(e => query[MessengerUser].insert(e))
        }
        .transact(transactor)
        .unsafeRunSync

    assert(insert == List(1, 1))
  }

  test("Test successful login") {
    val auth   = Authorize(0, "test")
    val result = AuthService.authorize(auth).value.unsafeRunSync

    result match {
      case Some(value) => true
      case None        => false
    }
  }

  test("Test incorrect login") {
    val auth   = Authorize(0, "INCORRECT PASSWORD")
    val result = AuthService.authorize(auth).value.unsafeRunSync

    result match {
      case Some(value) => false
      case None        => true
    }
  }

  /** Delete database after testing */
  override def afterAll() {
    new Directory(new File("./h2TestDir")).deleteRecursively()
  }
}
