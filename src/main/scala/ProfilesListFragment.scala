package local.nodens.remoterunner

import scalaz._
import Scalaz._

import android.app._
import android.content.Context
import android.os.Bundle
import android.view._
import android.widget._

import android.util.Log

import TypedResource._

class ProfilesListFragment extends ListFragment with ProfileView {
  lazy val profiles = new Profiles(getActivity.getContentResolver)
  lazy val keys = new Keys(getActivity.getContentResolver)

  override def onCreate(sis:Bundle) = {
    super.onCreate(sis)
    setListAdapter(new ProfilesAdapter(getActivity, profiles))
  }

  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    super.onCreateView(inflater, container, sis)
    inflater.inflate(R.layout.profile_list, null)
  }
  override def onListItemClick(l:ListView, v:View, pos:Int, id:Long) = {
    profileView(id)
  }
}

/** Implementation of ListAdapter interface wrapping a profiles map. */
class ProfilesAdapter(val activity:Activity, val profiles:Profiles) extends ListAdapter {
  import android.database.DataSetObserver
  import scala.collection.mutable.Buffer
  private val observers = Buffer.empty[DataSetObserver]
  //assume these two
  def areAllItemsEnabled = true
  def isEnabled(pos:Int) = true 
  def getCount = profiles.length
  def getItem(pos:Int) = (profiles get getItemId(pos)) getOrElse(null)
  def getItemId(pos:Int) = pos.asInstanceOf[Long]
  def getItemViewType(pos:Int) = 0
  /** This is the important func, it defines how profiles are displayed. */
  def getView(pos:Int, conv:View, parent:ViewGroup) = {
    val tv = replace(conv)
    (profiles get getItemId(pos)) foreach (profile => tv.setText(profile.someName))
    tv
  }
  def replace(view:View):TextView = view match {
      case (tv:TextView) => tv
      case _ => activity.getLayoutInflater.inflate(TR.layout.profile_item, null).asInstanceOf[TextView]
  }

  def getViewTypeCount = 1
  def hasStableIds = true
  def isEmpty = profiles.isEmpty
  def registerDataSetObserver(observer:DataSetObserver) = observers += observer
  def unregisterDataSetObserver(observer:DataSetObserver) = observers -= observer
  def onChanged() = observers foreach (_.onChanged())
  def onInvalidated() = observers foreach (_.onInvalidated())
}
