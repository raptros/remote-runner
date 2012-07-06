package local.nodens.remoterunner

import scalaz._
import Scalaz._

import android.app._
import android.content.Context
import android.os.Bundle
import android.view._
import android.widget._

import android.util.Log

trait OnClickMaker {
  def handle(button:Button)(handler:(View => Unit)):Unit = button.setOnClickListener(
    new View.OnClickListener {
      def onClick(v:View) = handler(v)
    })

  def handle1(button:Button)(handler: => Unit):Unit = handle(button)((v:View) => handler)
}

trait FragTrans {
  def getActivity:Activity
  def goBack():Unit = getActivity.getFragmentManager.popBackStack()
  def doFragTrans(f:(FragmentTransaction => Unit)):Unit = {
    val trans = getActivity.getFragmentManager.beginTransaction()
    f(trans)
    trans.commit()
  }
}
@EnhanceStrings
trait ProfileView extends FragTrans {
  def profileView(id:Long):Unit = {
    Log.d(TAG, "entering profile #id")
    doFragTrans {
      _.replace(R.id.primary_area, ProfileFragment(id))
      .addToBackStack(PROFILE_VIEW)
    }
  }
}

@EnhanceStrings
trait ProfileEdit extends FragTrans {
  def profileEdit(id:Long) = {
    Log.d(TAG, "requested edit of profile #id")
    doFragTrans {
      _.replace(R.id.primary_area, EditProfileFragment(id.some))
      .addToBackStack(PROFILE_EDIT)
    }
  }
}

@EnhanceStrings
trait ProfileLaunch {
  def profiles:Profiles
  def keys:Keys
  def profileLaunch(id:Long) = {
    Log.d(TAG, "requested launch of profile #id")
  }
}
