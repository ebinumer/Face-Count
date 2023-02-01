package com.ebinumer.headcount

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar


fun View.showSnackBar(msg:String){
 Snackbar.make(this, msg, Snackbar.LENGTH_LONG).show()
}
fun Activity.showToast(msg:String){
    Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
}

fun View.showSnackBar(
    msg: String,
    length: Int,
    actionMessage: CharSequence?,
    action: (View) -> Unit
) {
    val snackbar = Snackbar.make(this, msg, length)
    if (actionMessage != null) {
        snackbar.setAction(actionMessage) {
            action(this)
        }.show()
    } else {
        snackbar.show()
    }
}
fun Bitmap.fixRotation(uri: Uri): Bitmap? {

    val ei = ExifInterface(uri.path.toString())

    val orientation: Int = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_UNDEFINED
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage( 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage( 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage( 270f)
        ExifInterface.ORIENTATION_NORMAL -> this
        else -> this
    }
}
fun Bitmap.rotateImage(angle: Float): Bitmap? {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(
        this, 0, 0, width, height,
        matrix, true
    )
}