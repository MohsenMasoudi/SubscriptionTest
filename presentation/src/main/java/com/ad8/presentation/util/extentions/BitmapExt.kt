package com.ad8.presentation.util.extentions

import android.graphics.Bitmap
import android.os.Environment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun Bitmap.getResizedBitmap( maxSize: Int=1000): Bitmap {
//    var width: Int = this.width
//    var height: Int = this.height
//    val bitmapRatio = width.toFloat() / height.toFloat()
//    if (bitmapRatio > 1) {
//        width = maxSize
//        height = (width / bitmapRatio).toInt()
//    } else {
//        height = maxSize
//        width = (height * bitmapRatio).toInt()
//    }
//    return Bitmap.createScaledBitmap(this, width, height, true)
    return this
}
fun Bitmap.bitmapToFile( fileNameToSave: String="temp"): File? { // File name like "image.png"
    //create a file to write bitmap data
    var file: File? = null
    return try {
        file = File(Environment.getExternalStorageDirectory().toString() + File.separator + fileNameToSave)
        file.createNewFile()

        //Convert bitmap to byte array
        val bos = ByteArrayOutputStream()
        this.compress(Bitmap.CompressFormat.PNG, 0, bos) // YOU can also save it in JPEG
        val bitmapdata = bos.toByteArray()

        //write the bytes in file
        val fos = FileOutputStream(file)
        fos.write(bitmapdata)
        fos.flush()
        fos.close()
        file
    } catch (e: Exception) {
        e.printStackTrace()
        file // it will return null
    }
}