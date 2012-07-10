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

/** Displays a profile, with buttons for edit and execute. */
@EnhanceStrings
class ProfileFragment extends Fragment with OnClickMaker with ProfileEdit with ProfileLaunch {
  lazy val profiles = new Profiles(getActivity.getContentResolver)
  lazy val keys = new Keys(getActivity.getContentResolver)
  var profileId:Option[Long] = None

  override def onSaveInstanceState(out:Bundle) = {
    super.onSaveInstanceState(out)
    profileId foreach {
      id => out.putLong(kProfileId, id)
    }
  }
  /** extract the profileID arg, either from sis or argument. */
  override def onCreate(sis:Bundle) = {
    super.onCreate(sis)
    profileId = (sis oLong kProfileId) orElse (getArguments oLong kProfileId)
  }

  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    val view = inflater.inflate(R.layout.profile_display, null)
    setHasOptionsMenu(true)
    prepareView(view)
    view
  }

  /** sets up the view by extracting the profile, setting textfields, and addingcallbacks to buttons.*/
  def prepareView(view:View):Unit = for {id <- profileId; profile <- profiles get id} {
    view.findView(TR.profile_name_view).setText(profile.someName)
    view.findView(TR.host_view).setText(profile.host)
    view.findView(TR.user_view).setText(profile.user)
    view.findView(TR.port_view).setText(profile.port.toString)
    view.findView(TR.command_view).setText(profile.command)
    view.findView(TR.password_view).setText {
      profile.password ? "<hidden>" | "<none>"
    }
  }

  
  /** menu dispatcher */
  private val menuDispatch:PartialFunction[Int, Unit] = {
    case R.id.menu_profile_launch => profileId |>| (profileLaunch(_))
    case R.id.menu_profile_edit => profileId |>| (profileEdit(_))
  }

  override def onOptionsItemSelected(item:MenuItem) = (menuDispatch lift item.getItemId) ? true | super.onOptionsItemSelected(item)

  /** menu! */
  override def onCreateOptionsMenu(menu:Menu, mi:MenuInflater) =  mi.inflate(R.menu.menu_profile_view, menu)

}

/** companion object for setting up a profilefragment with an arguments bundle. */
object ProfileFragment {
  def mkArg(id:Long):Bundle = {
    val bundle = new Bundle
    bundle.putLong(kProfileId, id)
    bundle
  }

  def apply(id:Long) = {
    val frag = new ProfileFragment
    frag.setArguments(mkArg(id))
    frag
  }
}

/** Enables editing of a profile. */
@EnhanceStrings
class EditProfileFragment extends Fragment with OnClickMaker with ProfileView with ProfileDelete {
  lazy val profiles = new Profiles(getActivity.getContentResolver)
  lazy val keys = new Keys(getActivity.getContentResolver)

  var fromProfileId:Option[Long] = None

  override def onSaveInstanceState(out:Bundle) = {
    super.onSaveInstanceState(out)
    fromProfileId foreach {
      id => out.putLong(kFromProfileId, id)
    }
  }

  override def onCreate(sis:Bundle) = {
    super.onCreate(sis)
    fromProfileId = (sis oLong kFromProfileId) orElse (getArguments oLong kFromProfileId)
  }

  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    val view = inflater.inflate(R.layout.profile_edit, null)
    setHasOptionsMenu(true)
    view
  }
  override def onStart() = {
    super.onStart()
    fromProfileId flatMap(profiles get _) foreach(setProfile(getView, _))
  }

  /** fills in the fields for modifying an existing profile. */
  def setProfile(view:View, profile:Profile) = {
    view.findView(TR.profile_name_edit).setText(profile.name)
    view.findView(TR.host_edit).setText(profile.host)
    view.findView(TR.user_edit).setText(profile.user)
    view.findView(TR.port_edit).setText(profile.port.toString)
    view.findView(TR.command_edit).setText(profile.command)
    profile.password foreach (view.findView(TR.password_edit).setText(_))
  }

  /** extracts a new Profile from the fields.*/
  def getProfile(view:View):Profile = Profile(
    name = view.findView(TR.profile_name_edit).getText.toString,
    host = view.findView(TR.host_edit).getText.toString,
    user = view.findView(TR.user_edit).getText.toString,
    port = view.findView(TR.port_edit).getText.toString.toInt,
    command = view.findView(TR.command_edit).getText.toString,
    password = {
      val pw = view.findView(TR.password_edit).getText.toString
      (!pw.isEmpty) option pw
    })

  /** internal callback - cancels the edit. */
  def profileEditCancel(id:Option[Long]):Unit = {
    Log.d(TAG, "#id?[edit of profile #id|creation of new profile] cancelled")
    goBack()
  }

  /** internal callback - saves the new/editted profile, goes back.*/
  def profileEditSave(profile:Profile, fromId:Option[Long]):Unit = {
    Log.d(TAG, "profile #fromId?[edit of #it|creation of new profile] completed, saving")
    fromId some(profiles.update(_, profile)) none(profiles += profile)
    goBack()
    fromId ifNone (profileView(profiles.length - 1))
  }

  /** menu dispatcher */
  private val menuDispatch:PartialFunction[Int, Unit] = {
    case R.id.menu_profile_edit_save => profileEditSave(getProfile(getView), fromProfileId)
    case R.id.menu_profile_edit_delete => fromProfileId |>| (id => profileDelete(Seq(id)))
  }

  override def onOptionsItemSelected(item:MenuItem) = (menuDispatch lift item.getItemId) ? true | super.onOptionsItemSelected(item)

  /** menu! */
  override def onCreateOptionsMenu(menu:Menu, mi:MenuInflater) = {
    mi.inflate(R.menu.menu_profile_edit, menu)
    fromProfileId ifNone (menu.removeItem(R.id.menu_profile_edit_delete))
  }
}

/** companion object enables creation of an EditProfileFragment with arguments for target profile and optional profile to edit.*/
object EditProfileFragment {
  def mkArg(fromId:Option[Long]):Bundle = {
    val bundle = new Bundle
    fromId foreach (bundle.putLong(kFromProfileId, _))
    bundle
  }
  def apply(fromId:Option[Long]=None) = {
    val frag = new EditProfileFragment
    frag.setArguments(mkArg(fromId))
    frag
  }
}
