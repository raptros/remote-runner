package local.nodens.remoterunner
import com.lamerman._

import scalaz._
import Scalaz._

import android.app._
import android.content.{Context, DialogInterface, Intent}
import android.os.Bundle
import android.view._
import android.widget._

import android.util.{Log, AttributeSet}

import TypedResource._

import scala.collection.script.{Update, Include, Remove, Message, Index => SIndex}
import scala.collection.mutable.{Publisher, Subscriber}

@EnhanceStrings
trait KeyMgr extends FragTrans with Subscriber[Message[Int], Publisher[Message[Int]]] {
  val IMPORT_KEY = 0
  //one of those things that implementers must have.
  def startActivityForResult(intent:Intent, reqCode:Int)

  /** Bring up a key deletion dialog.*/
  def keyDelete(ids:Seq[Long]) = {
    Log.d(TAG, "requested delete of keys #ids")
    val df = DeleteKeyDF(ids)
    df.publisher.subscribe(this)
    df.show(getActivity.getFragmentManager, "dialog")
  }

  def keyPassEdit(id:Long) = {
    Log.d(TAG, "ediiting password of key #id")
    val df = PassEditDF(id)
    df.show(getActivity.getFragmentManager, "pass_dialog")
  }

  def keyImport() = {
    Log.d(TAG, "requesting import of new key")
    val intent = new Intent(getBaseContext(), classOf[FileDialog])
    intent.putExtra(FileDialog.START_PATH, "/sdcard")
    intent.putExtra(FileDialog.CAN_SELECT_DIR, false)
    intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
    startActivityForResult(intent, IMPORT_KEY)
  }

  def importReturned(resCode:Int, data:Intent) = if (resCode == Activity.RESULT_OK) {

  } else Log.d(TAG, "import failed")

  def onDeleteYes() = { }

  def notify(pub:Publisher[Message[Int]], msg:Message[Int]) = {
    msg.isInstanceOf[Remove[_]] when onDeleteYes()
    pub.removeSubscription(this)
  }
}


/** Dialog for deleting keys. */
@EnhanceStrings
class DeleteKeyDF extends DialogFragment {
  lazy val publisher = new Publisher[Message[Int]] { def doPublish(msg:Message[Int]) = publish(msg)  }
  lazy val keys = new Keys(getActivity.getContentResolver)

  def args:Seq[Long] = (getArguments oLongSeq kKeyArray) getOrElse Seq.empty[Long]

  override def onCreateDialog(sis:Bundle) = {
    val profileCount = args.length
    val onlyOne = args.length == 1
    (new AlertDialog.Builder(getActivity()))
    .setTitle("Delete Keys")
    .setMessage("Are you sure you want to delete #onlyOne?[this key|these #profileCount keys]?")
    .setNegativeButton("Cancel", DoCancel)
    .setPositiveButton("OK", DoDelete)
    .create()
  }

  def doDelete() = {
    args.sorted.reverse foreach (keys -= _)
    publisher.doPublish(new Remove(args.length))
  }

  override def onCancel(dialog:DialogInterface) = {
    super.onCancel(dialog)
    publisher.doPublish(NoOp)
  }
  //override def onDismiss(dialog:DialogInterface) =  super.onDismiss(dialog)

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

object DeleteKeyDF {
  import scala.collection.JavaConversions._
  def apply(ids:Seq[Long]) = {
    val bundle = new Bundle
    bundle.putLongArray(kKeyArray, ids.toArray)
    val frag = new DeleteKeyDF
    frag.setArguments(bundle)
    frag
  }
}


/** Dialog for editing the password of keys. */
@EnhanceStrings
class PassEditDF extends DialogFragment {
  lazy val publisher = new Publisher[Message[Int]] { def doPublish(msg:Message[Int]) = publish(msg)  }
  lazy val keys = new Keys(getActivity.getContentResolver)

  /** need to be careful with this! */
  lazy val textEdit = { 
    val te = new EditText(getActivity)
    te.setHint("Password")
    te.setTransformationMethod(new android.text.method.PasswordTransformationMethod)
    args flatMap (keys get _) flatMap(_.password) foreach (te.setText(_))
    te
  }

  def args:Option[Long] = (getArguments oLong kKeyId)

  override def onCreateDialog(sis:Bundle) = {
    (new AlertDialog.Builder(getActivity()))
    .setTitle("Set Key Password")
    .setView(textEdit)
    .setNegativeButton("Cancel", DoCancel)
    .setPositiveButton("OK", DoUpdate)
    .create()
  }

  def doSetPassword() = for {id <- args;  key <- keys get id} {
    val text:String = textEdit.getText().toString
    val oPass = text.isEmpty option(text)
    Key(privateKey = key.privateKey, name = key.name,
      password = oPass) |> (keys.update(id, _))
  }

  object DoCancel extends DialogInterface.OnClickListener {
    def onClick(dialog:DialogInterface, which:Int) = dialog.cancel()
  }
  object DoUpdate extends DialogInterface.OnClickListener {
    def onClick(dialog:DialogInterface, which:Int) = {
      Log.d(TAG, "updating #args")
      doSetPassword()
      dismiss()
    }
  }
}

object PassEditDF {
  import scala.collection.JavaConversions._
  def apply(id:Long) = {
    val bundle = new Bundle
    bundle.putLong(kKeyId, id)
    val frag = new PassEditDF
    frag.setArguments(bundle)
    frag
  }
}


