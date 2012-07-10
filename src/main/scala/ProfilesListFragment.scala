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
    //view.setOnItemLongClickListener(this)
    view
  }
  override def onListItemClick(l:ListView, v:View, pos:Int, id:Long) = {
    profileView(id)
  }

  def onItemCheckedStateChanged(mode:ActionMode, pos:Int, id:Long, checked:Boolean) = {
    mode.invalidate()
  }

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

/** class for a layout that contains a checkbox. This allows it to implement checkable, 
  * which is sufficient to allow the checkmark driven CAM, while also allowing the checkbox itself
  * to be a clickable entry point.
  */
class MyCheckableView(context:Context, attrs:AttributeSet, defStyle:Int) extends LinearLayout(context, attrs, defStyle) with Checkable {
  def this(context:Context, attrs:AttributeSet) = this(context, attrs, 0)
  def this(context:Context) = this(context, null)
  def button = this.findView(TR.profile_item_check_box)
  def isChecked = button.isChecked
  def setChecked(checked:Boolean) = button.setChecked(checked)
  def toggle = button.toggle
  
  def notifyListView(lv:ListView, state:Boolean) = {
    val pos = lv.getPositionForView(this)
    lv.setItemChecked(pos, state)
  }
  override def onFinishInflate = {
    super.onFinishInflate()
    button.setOnCheckedChangeListener { 
      new CompoundButton.OnCheckedChangeListener {
        def onCheckedChanged(buttonVew:CompoundButton, state:Boolean) = getParent.isInstanceOf[ListView] when {
          notifyListView(getParent.asInstanceOf[ListView], state)
        }
      }
    }
  }
}

