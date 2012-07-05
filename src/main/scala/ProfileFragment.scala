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
class ProfileFragment extends Fragment with OnClickMaker with FragTrans {
  lazy val profiles = new Profiles(getActivity.getContentResolver)
  lazy val keys = new Keys(getActivity.getContentResolver)

  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    val view = inflater.inflate(R.layout.profile_display, null)
    prepareView(view)
    view
  }

  def profileId:Option[Long] =  Option(getArguments) flatMap {
    (b:Bundle) => (b containsKey kProfileId) option (b getLong kProfileId)
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
    handle1(view.findView(TR.button_edit)) {
      profileEdit(id, profile)
    }
    handle1(view.findView(TR.button_launch)) {
      profileLaunch(id, profile)
    }
  }

  /** internal callback to push into an editing fragment.*/
  def profileEdit(id:Long, profile:Profile) = {
    Log.d(TAG, "requested edit of profile #id")
    val already = getActivity.getFragmentManager.findFragmentById(android.R.id.content)
    doFragTrans {
      _.remove(already).add(android.R.id.content, EditProfileFragment(id.some))
      .addToBackStack(null)
    }
  }

  def profileLaunch(id:Long, profile:Profile) = {
    Log.d(TAG, "requested launch of profile #id")
  }
}

/** companion object for setting up a profilefragment with an arguments bundle. */
object ProfileFragment {
  def apply(id:Long) = {
    val bundle = new Bundle
    bundle.putLong(kProfileId, id)
    val frag = new ProfileFragment
    frag.setArguments(bundle)
    frag
  }
}

/** Enables editing of a profile. */
@EnhanceStrings
class EditProfileFragment extends Fragment with OnClickMaker with ProfileView {
  lazy val profiles = new Profiles(getActivity.getContentResolver)
  lazy val keys = new Keys(getActivity.getContentResolver)

  def getArg(arg:String):Option[Long] =  Option(getArguments) flatMap {
    (b:Bundle) => (b containsKey arg) option (b getLong arg)
  }

  override def onCreateView(inflater:LayoutInflater, container:ViewGroup, sis:Bundle) = {
    val view = inflater.inflate(R.layout.profile_edit, null)
    handle1(view.findView(TR.button_cancel)) {
      profileEditCancel(getArg(kFromProfileId))
    }
    handle1(view.findView(TR.button_save)) {
      profileEditSave(getProfile(view), getArg(kFromProfileId))
    }
    getArg(kFromProfileId) flatMap(profiles get _) foreach(setProfile(view, _))
    view
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
}

/** companion object enables creation of an EditProfileFragment with arguments for target profile and optional profile to edit.*/
object EditProfileFragment {
  def apply(fromId:Option[Long]=None) = {
    val bundle = new Bundle
    fromId foreach (bundle.putLong(kFromProfileId, _))
    val frag = new EditProfileFragment
    frag.setArguments(bundle)
    frag
  }
}
