package local.nodens.remoterunner

import scalaz._
import Scalaz._

import android.app._
import android.content.{Context, DialogInterface}
import android.os.Bundle
import android.view._
import android.widget._

import scala.collection.script.{Update, Include, Remove, Message, Index => SIndex}
import scala.collection.mutable.{Publisher, Subscriber}

import android.util.Log

//traits for fragments; these traits do things to profiles.

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
  def profileNew() = {
    Log.d(TAG, "requested creation of new profile")
    doFragTrans {
      _.replace(R.id.primary_area, EditProfileFragment(None))
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

@EnhanceStrings
trait ProfileDelete extends FragTrans with Subscriber[Message[Int], Publisher[Message[Int]]] {
  def profileDelete(ids:Seq[Long]) = {
    Log.d(TAG, "requested delete of profiles #ids")
    val df = DeleteProfileDF(ids)
    df.publisher.subscribe(this)
    df.show(getActivity.getFragmentManager, "dialog")
  }
  def onDeleteYes() = { }

  def notify(pub:Publisher[Message[Int]], msg:Message[Int]) = {
    msg.isInstanceOf[Remove[_]] when onDeleteYes()
    pub.removeSubscription(this)
  }
}

//the profile delete tool.

@EnhanceStrings
class DeleteProfileDF extends DialogFragment {
  lazy val publisher = new Publisher[Message[Int]] {
    def doPublish(msg:Message[Int]) = publish(msg)
  }

  lazy val profiles = new Profiles(getActivity.getContentResolver)
  def args:Seq[Long] = (getArguments oLongSeq kProfileArray) getOrElse Seq.empty[Long]

  override def onCreateDialog(sis:Bundle) = {
    val profileCount = args.length
    val onlyOne = args.length == 1
    (new AlertDialog.Builder(getActivity()))
    .setTitle("Delete Profiles")
    .setMessage("Are you sure you want to delete #onlyOne?[this profile|these #profileCount profiles]?")
    .setNegativeButton("Cancel", DoCancel)
    .setPositiveButton("OK", DoDelete)
    .create()
  }

  def doDelete() = {
    val fragman = getActivity.getFragmentManager
    val count = fragman.getBackStackEntryCount
    (0 until count) >| fragman.popBackStack()
    args.sorted.reverse foreach (profiles -= _)
    publisher.doPublish(new Remove(count))
  }

  override def onCancel(dialog:DialogInterface) = {
    super.onCancel(dialog)
    publisher.doPublish(NoOp)
  }
  override def onDismiss(dialog:DialogInterface) = {
    super.onDismiss(dialog)
  }
  object DoCancel extends DialogInterface.OnClickListener {
    def onClick(dialog:DialogInterface, which:Int) = dialog.cancel()
  }
  object DoDelete extends DialogInterface.OnClickListener {
    def onClick(dialog:DialogInterface, which:Int) = {
      Log.d(TAG, "deleteing #args")
      doDelete()
      dismiss()
    }
  }
}

case object NoOp extends Message[Int]

object DeleteProfileDF {
  import scala.collection.JavaConversions._
  def apply(ids:Seq[Long]) = {
    val bundle = new Bundle
    bundle.putLongArray(kProfileArray, ids.toArray)
    val frag = new DeleteProfileDF
    frag.setArguments(bundle)
    frag
  }
}

