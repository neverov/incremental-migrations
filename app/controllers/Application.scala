package controllers

import play.api.mvc._
import scala.concurrent.ExecutionContext.Implicits.global
import reactivemongo.api._
import play.api.libs.json.Json._

object Application extends Controller {

  def index = Action.async {

    Animal.all() map (a => Ok(prettyPrint(obj("animals" -> a))))
  }

  object mongo {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    val db = connection.db("incremental-migrations")
  }
}