package local.nodens.remoterunner

import scalaz._
import Scalaz._

import android.app._
import android.content.Context
import android.os.Bundle
import android.view._
import android.widget._
import java.util.UUID

import android.util.Log

@EnhanceStrings
class RemoteRunnerActivity extends Activity with TypedActivity with FragmentManager.OnBackStackChangedListener {
  lazy val keys = new Keys(getContentResolver)
  lazy val profiles = new Profiles(getContentResolver)

  override def onCreate(sis:Bundle) {
    super.onCreate(sis)
    getFragmentManager.addOnBackStackChangedListener(this)
    setContentView(R.layout.main)
    Option(sis) ifNone {
      Log.d(TAG, "about to do initial fragment transaction")
      doFragTrans (_.replace(R.id.primary_area, new ProfilesListFragment))
    }
  }

  override def onCreateOptionsMenu(menu:Menu):Boolean = {
    getMenuInflater.inflate(R.menu.menu_main, menu)
    true
  }

  def doFragTrans(f:(FragmentTransaction => Unit)):Unit = {
    val trans = getFragmentManager.beginTransaction()
    f(trans)
    trans.commit()
  }

  private val menuDispatch:PartialFunction[Int, Unit] = {
    case android.R.id.home => goBack()
    case R.id.menu_profile_add => profileNew()
  }
  override def onOptionsItemSelected(item:MenuItem) = (menuDispatch lift item.getItemId) ? true | super.onOptionsItemSelected(item)

  def goBack():Unit = getFragmentManager.popBackStack()

  def profileView(id:Long):Unit = {
    Log.d(TAG, "entering profile #id")
    doFragTrans {
      _.replace(R.id.primary_area, ProfileFragment(id))
      .addToBackStack(PROFILE_VIEW)
    }
  }

  def profileNew():Unit = {
    Log.d(TAG, "creating new profile")
    doFragTrans {
      _.replace(R.id.primary_area, EditProfileFragment(None))
      .addToBackStack(PROFILE_EDIT)
    }
  }

  def onBackStackChanged() = {
    getActionBar.setDisplayHomeAsUpEnabled(getFragmentManager.getBackStackEntryCount > 0)
  }

/*
  def setProfile(id:Long, data:Profile):Unit = profiles += (id -> data)

  def deleteProfile(id:Long):Unit = profiles -= id
*/
  override def onBackPressed() = if (getFragmentManager.getBackStackEntryCount > 0) goBack() else super.onBackPressed()
}

