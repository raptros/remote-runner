package local.nodens

package object remoterunner {
  import android.net.Uri
  import android.content.{ContentUris, ContentResolver}
  import scala.collection.JavaConversions._
  import scalaz._
  import Scalaz._
  val kProfileId = "profile_id"
  val kFromProfileId = "from_profile_id"

  val TAG = "RemoteRunner"
/*
  import net.liftweb.json._
  import Serialization.{read, write}
  implicit val formats = Serialization.formats(NoTypeHints)
*/
  val contentURI = (new Uri.Builder).scheme(ContentResolver.SCHEME_CONTENT).authority("local.nodens.remoterunner.provider").build

  def uri(table:String):Uri = contentURI.buildUpon.appendPath(table).build()
  def uriId(table:String, id:Long):Uri = contentURI.buildUpon.appendPath(table).appendPath(id.toString).build()

  def table(uri:Uri):String = {
    val table = uri.getPathSegments.toList.headOption flatMap (t => (DBConsts.tables contains t) option(t))
    table getOrElse (throw new IllegalArgumentException(uri toString))
  }

  def idGet(uri:Uri):Option[Long] = (uri.getPathSegments.toList lift 1)
  .flatMap (id => { () => id.toLong }.throws.toOption)

  def isCount(uri:Uri):Boolean = (uri.getPathSegments.toList lift 1) map(_ == "count") getOrElse(false)

}
