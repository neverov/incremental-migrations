package controllers

import play.api.libs.json.{Reads, JsObject}
import scala.concurrent.{Await, Future}
import reactivemongo.bson.BSONObjectID
import controllers.MigrationHelpers._
import play.modules.reactivemongo.json.collection.JSONCollection
import play.api.libs.json.JsObject
import scala.Some
import scala.concurrent.ExecutionContext.Implicits.global
import play.modules.reactivemongo.json.BSONFormats._

abstract class SyncMigration(override val from: Int, override val to: Int) extends Migration {
  import scala.concurrent.duration._

  val timeout = 1 second

  def syncUp(instance: JsObject): Future[Any]

  def up(instance: JsObject) = {
    Await.result(syncUp(instance), timeout)
  }
}

trait Migration {
  val from: Int
  val to: Int

  def up(instance: JsObject): Unit
}

object MigrationHelpers {
  def getVersion(json: JsObject) = (json \ "version").asOpt[Int] match {
    case Some(version) => version
    case None => 1
  }

  def getId(json: JsObject) = (json \ "_id").asOpt[BSONObjectID] match {
    case Some(id) => id
    case None => throw new IllegalStateException("Where the fuck is my id?")
  }
}

class Versioned(val version: Int = 0)

trait MigrationSupport[T <: Versioned] {

  val migrations: List[Migration]

  val latestVersion: Int

  require ((1 until latestVersion) forall (i => migrations.exists(_.from == i)))

  def bumpAll(collection: JSONCollection)(query: JsObject)(implicit reads: Reads[T]) = {
    collection.find(query).cursor[JsObject].collect[List]() flatMap {
      case results =>
        results map migrate
        collection.find(query).cursor[T].collect[List]()
    }
  }

  def migrate(json: JsObject) = {
    val currentVersion = getVersion(json)
    println(s"trying to migrate $json from: $currentVersion to version: $latestVersion")

    if (currentVersion < latestVersion) {
      migrations.filter(m => currentVersion < m.to && m.from <= latestVersion) map { m =>
        m.up(json)
        println(s"applied migration from: ${m.from} to: ${m.to}")
      }
    } else {
      println(s"already at latest version: $currentVersion")
    }
  }
}