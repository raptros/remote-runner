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
class RemoteRunnerActivity extends Activity with TypedActivity {
  lazy val keys = new Keys(getContentResolver)
  lazy val profiles = new Profiles(getContentResolver)

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    //setContentView(R.layout.main)
    Log.d(TAG, "about to do initial fragment transaction")
    doFragTrans (_.replace(android.R.id.content, new ProfilesListFragment))
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
    case R.id.menu_add_profile => profileNew()
  }
  override def onOptionsItemSelected(item:MenuItem) = (menuDispatch lift item.getItemId) ? true | super.onOptionsItemSelected(item)

  def goBack():Unit = getFragmentManager.popBackStack()

  def profileView(id:Long):Unit = {
    Log.d(TAG, "entering profile #id")
    val already = getFragmentManager.findFragmentById(android.R.id.content)
    doFragTrans {
      _.replace(android.R.id.content, ProfileFragment(id))
      .addToBackStack(null)
    }
  }

  def profileNew():Unit = {
    Log.d(TAG, "creating new profile")
    val already = getFragmentManager.findFragmentById(android.R.id.content)
    doFragTrans {
      _.replace(android.R.id.content, EditProfileFragment(None))
      .addToBackStack(null)
    }
  }

/*
  def setProfile(id:Long, data:Profile):Unit = profiles += (id -> data)

  def deleteProfile(id:Long):Unit = profiles -= id
*/
  override def onBackPressed() = if (getFragmentManager.getBackStackEntryCount > 0) goBack() else super.onBackPressed()
}

