package local.nodens

package object remoterunner {
  import android.net.Uri
  import android.content.{ContentUris, ContentResolver}
  import scala.collection.JavaConversions._
  import scala.collection.immutable.Map
  import android.os.Bundle
  import scalaz._
  import Scalaz._
  val kProfileId = "profile_id"
  val kFromProfileId = "from_profile_id"

  val TAG = "RemoteRunner"

  val PROFILE_VIEW = "profile_view"
  val PROFILE_EDIT = "profile_edit"

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

  class BundleWrapper(bundle:Bundle) {
    def oLong(key:String):Option[Long] = Option(bundle) flatMap (b => (b containsKey key) option (b getLong key))
    def oString(key:String):Option[String] = Option(bundle) flatMap (b => (b containsKey key) option (b getString key))
  }

  implicit def bundle2BundleWrapper(bundle:Bundle) = new BundleWrapper(bundle)
}
