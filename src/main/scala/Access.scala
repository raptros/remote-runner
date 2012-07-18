package local.nodens.remoterunner
import scalaz._
import Scalaz._

import scala.collection.mutable.{Buffer, ArrayBuffer}
import scala.collection.JavaConversions._

import android.database.Cursor
import android.database.sqlite._
import android.content.{Context, ContentResolver, ContentValues}
import android.util.Log

@EnhanceStrings
case class Profile(
  user:String,
  host:String,
  port:Int=22,
  command:String="",
  name:String="",
  password:Option[String]=None,
  keyId:Option[Long]=None) {
  def someName = name.isEmpty ? "#user@#host:#port $ #command" | name
  override def toString = someName
}

class Profiles(resolver:ContentResolver) extends StorageResolver[Profile](resolver) {
  val table = "profiles"
  def extract(row:CursorRow):Option[Profile] = {
    (row getString "user") |@|
    (row getString "host") |@|
    (row getInt "port") |@|
    (row getString "command") |@|
    (row getString "name") |@|
    Some(row getString "password") |@|
    Some(row getLong "keyId")
  } apply { Profile }

  def prepare(profile:Profile):ContentValues = {
    val cv = new ContentValues()
    profile match {
      case Profile(user, host, port, command, name, password, keyId) => {
        cv put ("user", user)
        cv put ("host", host)
        cv put ("port", port.asInstanceOf[java.lang.Integer])
        cv put ("command", command)
        cv put ("name", name)
        password |>| (cv put ("password", _:String))
        keyId |>| (kid => cv put ("keyId", kid.asInstanceOf[java.lang.Long]))
      }
    }; cv
  }
}

case class Key(
  privateKey:String,
  name:String="id_rsa",
  password:Option[String]=None) {
  override def toString = name
}

class Keys(resolver:ContentResolver) extends StorageResolver[Key](resolver) {
  val table = "keys"
  def extract(row:CursorRow):Option[Key] = {
    (row getString "privateKey") |@|
    (row getString "name") |@|
    Some(row getString "password")
  } apply { Key }

  def prepare(key:Key):ContentValues = {
    val cv = new ContentValues()
    key match {
      case Key(privateKey, name, password) => {
        cv put ("privateKey", privateKey)
        cv put ("name", name)
        cv put ("password", password.getOrElse(null))
      }
    }; cv
  }
}

@EnhanceStrings
class CursorRow(cursor:Cursor) {
  private val cols = cursor.getColumnNames
  Log.d(TAG, "cursor has columns (#cols[#it]{, }*) and #cursor.getCount rows.")
  private val colMap = cols.zipWithIndex.toMap[String, Int]
  private def get[A](name:String)(f:(Int => A)):Option[A] = {
    val check = (i:Int) => (!(cursor isNull i)).option(i)
    val retrieve = (i:Int) => {() => f(i)}.throws.toOption
    colMap.get(name).flatMap(check).flatMap(retrieve)
  }

  def getInt(name:String):Option[Int] = get(name)(cursor.getInt(_))
  def getString(name:String):Option[String] = get(name)(cursor.getString(_))
  def getLong(name:String):Option[Long] = get(name)(cursor.getLong(_))
}

@EnhanceStrings
abstract class StorageResolver[A](val resolver:ContentResolver)(implicit manifest:Manifest[A]) {
  import DBConsts._
  implicit def cursor2CursorRow(cursor:Cursor) = new CursorRow(cursor)

  def table:String

  def extract(row:CursorRow):Option[A]

  def prepare(a:A):ContentValues

  def ids:ArrayBuffer[Long] = {
    val ids = ArrayBuffer.empty[Long]
    val cursor = resolver.query(uri(table), Array("#colId"), null, null, null)
    cursor.moveToFirst()
    while (!cursor.isAfterLast) {ids += cursor.getLong(0); cursor.moveToNext()}
    cursor.close()
    ids
  }

  def +=(a:A) = {
    val vNew = prepare(a)
    val res = resolver.insert(uri(table), vNew).toString
    Log.d(TAG, "result of insert was #res")
    this
  }
  
  def tailOpt:Option[A] = get(length - 1)

  def update(idx:Long, a:A):Unit = {
    val vNew = prepare(a)
    resolver.update(uriId(table, ids(idx.toInt)), vNew, null, null)
  }

  def -=(idx:Long):Unit = {
    val it = get(idx)
    resolver.delete(uriId(table, ids(idx.toInt)), null, null)
  }
  def apply(idx:Long):A = get(idx.toInt).get

  def get(idx:Long):Option[A] = {
    //yes, we want all the columns
    Log.d(TAG, "fetching #idx from #table")
    val cursor = resolver.query(uriId(table, ids(idx.toInt)), null, null, null, null)
    cursor.moveToFirst()
    val got = extract(cursor)
    Log.d(TAG, "got #got?[#it|nothing]")
    cursor.close()
    got
  }
  
  def contains(idx:Long):Boolean = (idx >= 0) && (idx < length) 

  class TableIterator extends Iterator[A] {
    val cursor = resolver.query(uri(table), null, null, null, null) //"SELECT * FROM #table;", null)
    val wrapper = new CursorRow(cursor)
    cursor.moveToFirst()
    def hasNext = (cursor.getCount > 0) && !cursor.isClosed()
    def next() = if (cursor.getCount == 0) null else {
      extract(wrapper).get
    }
  }
  def iterator = (0 until length).toStream.flatMap(get(_)).toIterator

  def length = {
    val cursor = resolver.query(uri(table).buildUpon.appendPath("count").build(), null, null, null, null)
    cursor.moveToFirst()
    val count = cursor getInt 0
    cursor.close(); count
  }

  def foreach[U](f:(A)=>U):Unit = iterator.foreach(f)

  def isEmpty = length == 0

}



