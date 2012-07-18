package local.nodens.remoterunner

import scalaz._
import Scalaz._

import android.app._
import android.content.Context
import android.os.Bundle
import android.view._
import android.widget._

import android.util.{Log, AttributeSet}

import TypedResource._

/** Displays the list of profiles. */
class ProfilesListFragment extends ListFragment 
with ProfileView with ProfileEdit with ProfileLaunch with ProfileDelete
with AbsListView.MultiChoiceModeListener {

  lazy val profiles = new Profiles(getActivity.getContentResolver)
  lazy val keys = new Keys(getActivity.getContentResolver)

  /** sets up the adapter (an array adapter) */
  override def onCreate(sis:Bundle) = {
    super.onCreate(sis)
    val adapter = new ArrayAdapter[Profile](getActivity, R.layout.profile_item, android.R.id.text1)
    profiles foreach (adapter.add(_))
    setListAdapter(adapter)
  }

  /** loads the view from xml, attaches the mode listener. */
  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    super.onCreateView(inflater, container, sis)
    val view = inflater.inflate(R.layout.profile_list, null).asInstanceOf[ListView]
    view.setMultiChoiceModeListener(this)
    view.setItemsCanFocus(true)
    setHasOptionsMenu(true)
    view
  }

  /** menu stuff. */
  private val menuDispatch:PartialFunction[Int, Unit] = {
    case R.id.menu_profile_add => profileNew()
    case R.id.menu_key_list => doFragTrans {
      _.replace(R.id.primary_area, new KeyListFragment)
      .addToBackStack("keys")
    }
  }
  override def onOptionsItemSelected(item:MenuItem) = (menuDispatch lift item.getItemId) ? true | super.onOptionsItemSelected(item)
  override def onCreateOptionsMenu(menu:Menu, mi:MenuInflater) = {
    mi.inflate(R.menu.menu_profiles_list, menu)
    true
  }

  override def onListItemClick(l:ListView, v:View, pos:Int, id:Long) = { profileView(id) }

  def onItemCheckedStateChanged(mode:ActionMode, pos:Int, id:Long, checked:Boolean) = { mode.invalidate() }

  override def onStart() = {
    super.onStart()
    fixAdapter()
  }
  
  override def onDeleteYes() = fixAdapter()

  def fixAdapter() = {
    val aa = getListAdapter.asInstanceOf[ArrayAdapter[Profile]]
    aa.clear()
    profiles foreach (aa.add(_))
    getListView.invalidateViews()
  }

  /** CAB dispatcher */
  private val cabDispatch:PartialFunction[Int, Unit] = {
    case R.id.menu_profile_cab_edit => checkedItems.headOption foreach (profileEdit(_))
    case R.id.menu_profile_cab_launch => checkedItems foreach (profileLaunch(_))
    case R.id.menu_profile_cab_delete => { profileDelete(checkedItems) }
  }

  def onActionItemClicked(mode:ActionMode, item:MenuItem):Boolean = (cabDispatch lift item.getItemId) ? 
  {mode.finish(); true } | false

  def onCreateActionMode(mode:ActionMode, menu:Menu):Boolean = {
    mode.getMenuInflater.inflate(R.menu.menu_profile_cab, menu)
    true
  }

  def onDestroyActionMode(mode:ActionMode):Unit = {  }

  def onPrepareActionMode(mode:ActionMode, menu:Menu):Boolean = {
    !(selectedCount > 1) |> (menu.findItem(R.id.menu_profile_cab_edit).setVisible(_))
    true
  }

  def selectedCount:Int = getListView.getCheckedItemCount
  def checkedItems:Seq[Long] = {
    val sel = getListView.getCheckedItemPositions
    (0 until sel.size).toSeq flatMap {
      id => (sel valueAt id)  option(id.toLong)
    }
  }
  
}
