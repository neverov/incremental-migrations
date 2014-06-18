package controllers

import reactivemongo.bson.{BSONDocument, BSONObjectID}
import play.api.libs.json._
import controllers.Application.mongo
import play.api.libs.json.Json._
import reactivemongo.core.commands.FindAndModify
import scala.concurrent.ExecutionContext.Implicits.global
import MigrationHelpers._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.core.commands.Update
import play.api.libs.json.JsObject
import play.modules.reactivemongo.json.BSONFormats._

case class Animal(_id: BSONObjectID, name: String, age: Int, override val version: Int) extends Versioned

object Animal extends MigrationSupport[Animal] {

  val latestVersion = 3

  val migrations = List(Animal1to2, Animal2to3)

  def coll = mongo.db[JSONCollection]("animals")

  def one(id: BSONObjectID) = ???

  def all(query: JsObject = obj()) = bumpAll(coll)(query)

  implicit val format = Json.format[Animal]
}

object Animal1to2 extends SyncMigration(1, 2) {

  override def syncUp(instance: JsObject) = {
    mongo.db.command(FindAndModify(
      "animals",
      BSONDocument("_id" -> getId(instance)),
      Update(BSONDocument("$set" -> BSONDocument("owner" -> "", "version" -> to)), fetchNewObject = false)
    ))
  }
}

object Animal2to3 extends SyncMigration(2, 3) {

  override def syncUp(instance: JsObject) = {
    mongo.db.command(FindAndModify(
      "animals",
      BSONDocument("_id" -> getId(instance)),
      Update(BSONDocument("$unset" -> BSONDocument("owner" -> ""),  "$set" -> BSONDocument("version" -> to)), fetchNewObject = false)
    ))
  }
}

