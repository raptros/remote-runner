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
    val already = getActivity.getFragmentManager.findFragmentById(android.R.id.content)
    doFragTrans {
      _.replace(android.R.id.content, ProfileFragment(id))
      .addToBackStack(null)
    }
  }
}

