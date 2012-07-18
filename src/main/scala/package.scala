package local.nodens

package object remoterunner {
  import android.net.Uri
  import android.content.{ContentUris, ContentResolver}
  import scala.collection.JavaConversions._
  import scala.collection.immutable.Map
  import android.app._
  import android.view._
  import android.widget._
  import android.os.Bundle
  import scalaz._
  import Scalaz._
  val kProfileId = "profile_id"
  val kKeyId = "key_id"
  val kFromProfileId = "from_profile_id"
  val kProfileArray = "profile_array"
  val kKeyArray = "key_array"

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

  /** implicitly-created wrapper for a bundle, that deals with the possibiltiy of unavailability.*/
  class BundleWrapper(bundle:Bundle) {
    def oLong(key:String):Option[Long] = Option(bundle) flatMap (b => (b containsKey key) option (b getLong key))
    def oString(key:String):Option[String] = Option(bundle) flatMap (b => (b containsKey key) option (b getString key))
    def oLongSeq(key:String):Option[Seq[Long]] = Option(bundle) flatMap (b => (b containsKey key) option (b getLongArray key)) map (_.toSeq)
  }

  implicit def bundle2BundleWrapper(bundle:Bundle) = new BundleWrapper(bundle)
  
  /** Anything that wants to handle a button click.*/
  trait OnClickMaker {
    def handle(button:Button)(handler:(View => Unit)):Unit = button.setOnClickListener {
      new View.OnClickListener { def onClick(v:View) = handler(v) }
    }

    def handle1(button:Button)(handler: => Unit):Unit = handle(button)((v:View) => handler)
  }

  /** Trait that allows fragments to do activity-based fragment transactions */
  trait FragTrans {
    def getActivity:Activity
    def goBack():Unit = getActivity.getFragmentManager.popBackStack()
    def doFragTrans(f:(FragmentTransaction => Unit)):Unit = {
      val trans = getActivity.getFragmentManager.beginTransaction()
      f(trans);  trans.commit()
    }
  }

}
