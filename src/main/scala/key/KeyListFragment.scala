package local.nodens.remoterunner

import scalaz._
import Scalaz._

import android.app._
import android.content.{Context, Intent}
import android.os.Bundle
import android.view._
import android.widget._

import android.util.{Log, AttributeSet}

import TypedResource._

/** Displays the list of keys. */
class KeyListFragment extends ListFragment with KeyMgr 
with AbsListView.MultiChoiceModeListener {
  lazy val keys = new Keys(getActivity.getContentResolver)

  /** sets up the adapter (an array adapter) */
  override def onCreate(sis:Bundle) = {
    super.onCreate(sis)
    val adapter = new ArrayAdapter[Key](getActivity, R.layout.key_item, android.R.id.text1)
    keys foreach (adapter.add(_))
    setListAdapter(adapter)
  }

  /** loads the view from xml, attaches the mode listener. */
  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    super.onCreateView(inflater, container, sis)
    val view = inflater.inflate(R.layout.key_list, null).asInstanceOf[ListView]
    view.setMultiChoiceModeListener(this)
    view.setItemsCanFocus(true)
    setHasOptionsMenu(true)
    view
  }

  /** menu stuff. */
  private val menuDispatch:PartialFunction[Int, Unit] = {
    case R.id.menu_key_import => keyImport()
  }
  override def onOptionsItemSelected(item:MenuItem) = (menuDispatch lift item.getItemId) ? true | super.onOptionsItemSelected(item)

  override def onCreateOptionsMenu(menu:Menu, mi:MenuInflater) = {
    mi.inflate(R.menu.menu_key_list, menu)
    true
  }

  override def onListItemClick(l:ListView, v:View, pos:Int, id:Long) = { keyPassEdit(id) }

  def onItemCheckedStateChanged(mode:ActionMode, pos:Int, id:Long, checked:Boolean) = { mode.invalidate() }

  /** needed to make key import work: */
  override def onActivityResult(reqCode:Int, resCode:Int, data:Intent) = if (reqCode == IMPORT_KEY) importReturned(resCode, data) else
  super.onActivityResult(reqCode, resCode, data)

  override def onStart() = {
    super.onStart()
    fixAdapter()
  }
  
  override def onDeleteYes() = fixAdapter()

  def fixAdapter() = {
    val aa = getListAdapter.asInstanceOf[ArrayAdapter[Key]]
    aa.clear()
    keys foreach (aa.add(_))
    getListView.invalidateViews()
  }

  /** CAB dispatcher */
  private val cabDispatch:PartialFunction[Int, Unit] = {
    case R.id.menu_key_cab_delete => { keyDelete(checkedItems) }
  }

  def onActionItemClicked(mode:ActionMode, item:MenuItem):Boolean = (cabDispatch lift item.getItemId) ? 
  {mode.finish(); true } | false

  def onCreateActionMode(mode:ActionMode, menu:Menu):Boolean = {
    mode.getMenuInflater.inflate(R.menu.menu_key_cab, menu)
    true
  }

  def onDestroyActionMode(mode:ActionMode):Unit = {  }

  def onPrepareActionMode(mode:ActionMode, menu:Menu):Boolean = { false }

  def selectedCount:Int = getListView.getCheckedItemCount
  def checkedItems:Seq[Long] = {
    val sel = getListView.getCheckedItemPositions
    (0 until sel.size).toSeq flatMap {
      id => (sel valueAt id)  option(id.toLong)
    }
  }
}
