package local.nodens.remoterunner

import scalaz._
import Scalaz._

import scala.util.matching.Regex

import android.app._
import android.content._
import android.os.Bundle
import android.view._
import android.widget._

import android.database._
import android.database.sqlite._
import android.util.Log

import TypedResource._

import android.provider._
import android.net.Uri

import scala.collection.JavaConversions._

@EnhanceStrings
class RemoteRunnerProvider extends ContentProvider {
  lazy val helper = new StorageOpenHelper(getContext())
  lazy val db = helper.getWritableDatabase()

  def where(uri:Uri, selection:String):Option[String] = {
    val idClause = idGet(uri) map (id => "#BaseColumns._ID=#id")
    val whereAll = (idClause |@| " AND ".some |@| Option(selection)) apply (_ + _ + _) 
    whereAll <+> idClause <+> Option(selection) //first one that isn't None.
  }
  
  def transaction[A](f: =>A):A = {
    db.beginTransaction()
    val a = f
    db.endTransaction()
    a
  }
  
  def query(uri:Uri, projection:Array[String], selection:String, selectionArgs:Array[String], sortOrder:String):Cursor = isCount(uri) ? getCount(uri) | {
    Log.d(TAG, "query: #{uri.toString}")
    val builder = new SQLiteQueryBuilder
    table(uri) |> (builder.setTables(_))
    idGet(uri) foreach (id => builder.appendWhere("#BaseColumns._ID=#id"))
    builder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
  }

  def getCount(uri:Uri):Cursor = table(uri) |> (table => db.rawQuery("SELECT COUNT(*) FROM #table;", null))

  def insert(uri:Uri, values:ContentValues):Uri = {
    val newID = table(uri) |> (db.insert(_, null, values))
    ContentUris.withAppendedId(uri, newID)
  }

  def update(uri:Uri, values:ContentValues, selection:String, selectionArgs:Array[String]):Int = db.update(table(uri), values, 
    where(uri, selection) getOrElse(null), selectionArgs)

  def delete(uri:Uri, selection:String, selectionArgs:Array[String]) = transaction { 
    Log.d(TAG, "deleting #uri")
    val wh = where(uri, selection) getOrElse(null)
    Log.d(TAG, "running where: #wh")
    val count = db.delete(table(uri), wh, selectionArgs)
    db.setTransactionSuccessful()
    count
  }

  def getType(uri:Uri) = "vnd.cursor." + (idGet(uri) ? "item" | "dir") + "/vnd.local.nodens.provider." + table(uri)

  def onCreate:Boolean = {
    helper; true
  }
}

object CursorFactory extends SQLiteDatabase.CursorFactory {
  def newCursor(db:SQLiteDatabase, masterQuery:SQLiteCursorDriver, editTable:String, query:SQLiteQuery) = {
    new SQLiteCursor(masterQuery, editTable, query)
  }
}

@EnhanceStrings
class StorageOpenHelper(context:Context) extends SQLiteOpenHelper(context, DBConsts.DB_NAME, CursorFactory, DBConsts.DB_VERSION) {
  import DBConsts._
  override def onCreate(db:SQLiteDatabase) = {
    List(createKeys, createProfiles) foreach (db.execSQL(_))
  }

  override def onUpgrade(db:SQLiteDatabase, oldVersion:Int, newVersion:Int) = {
    //don't do anything right now - there aren't any db versions yet.
  }
}

@EnhanceStrings
object DBConsts {
  val colId = BaseColumns._ID
  val DB_NAME = "rrdb"
  val DB_VERSION = 2
  val tables = List("profiles", "keys")
  val colHost = "host"
  val createProfiles = "CREATE TABLE profiles (" +
  "#colId INTEGER PRIMARY KEY AUTOINCREMENT, " +
  "#colHost TEXT NOT NULL, " +
  "user TEXT NOT NULL, " +
  "port INTEGER DEFAULT 22, " +
  "command TEXT DEFAULT '', " +
  "name TEXT DEFAULT '', " +
  "password TEXT, " +
  "keyId INTEGER REFERENCES keys(#colId)" + ");"
  
  val createKeys = "CREATE TABLE keys (" +
  "#colId INTEGER PRIMARY KEY AUTOINCREMENT, " +
  "privateKey TEXT NOT NULL, " +
  "name TEXT DEFAULT 'id_rsa', " +
  "password TEXT" + ");"

}
