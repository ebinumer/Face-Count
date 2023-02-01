package com.ebinumer.headcount

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.ebinumer.headcount.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.io.FileDescriptor
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var filePhoto: File
    private lateinit var mBinding: ActivityMainBinding
    private var isCamera = false
    private var isCameraPermission = false
    private var faceDetector: FaceDetector? = null
    var imageUri: Uri? = null
    private  val FILE_NAME = "photo.jpg"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        onClickRequestPermission()
        initUi()
        btnClick()
    }

    private fun initUi() {
        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .enableTracking()
            .build()
        faceDetector = FaceDetection.getClient(highAccuracyOpts)
    }

    private fun btnClick() {
        mBinding.apply {
            btnGallery.setOnClickListener {
                val intent =
                    Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.type = "image/*"
                resultLauncher.launch(intent)
                isCamera = false
            }
            btnCamera.setOnClickListener {
                if (isCameraPermission) {
                    ImageFromCamera()
                } else {
                    onClickRequestPermission()
                }
            }
        }
    }

    private fun onClickRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                mBinding.btnCamera.showSnackBar(
                    getString(R.string.permission_granted),
                    Snackbar.LENGTH_SHORT,
                    null
                ) {

                }
                isCameraPermission = true

            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.CAMERA
            ) -> {
                mBinding.btnCamera.showSnackBar(
                    getString(R.string.permission_required),
                    Snackbar.LENGTH_SHORT,
                    getString(R.string.ok)
                ) {
                    //this will perform only when click ok btn
                    requestPermissionLauncher.launch(
                        android.Manifest.permission.CAMERA
                    )

                }
            }

            else -> {
                requestPermissionLauncher.launch(
                    android.Manifest.permission.CAMERA
                )
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            isCameraPermission = if (isGranted) {
                mBinding.btnCamera.showSnackBar("Permission Granted")
                Log.i("Permission: ", "Granted")
                true
            } else {
                mBinding.btnCamera.showSnackBar("Permission Denied")
                Log.i("Permission: ", "Denied")
                false
            }
        }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    mBinding.apply {
                        val data: Intent? = result.data
                        when {
                            isCamera -> {

                                val bitmap = uriToBitmap(imageUri!!)
                                imageView.setImageBitmap(bitmap)
                                bitmap?.let { runDetection(it) }
                            }
                            !isCamera -> {
                                imageUri = data?.data
                                val bitmap = uriToBitmap(imageUri!!)
                                imageView.setImageBitmap(bitmap)
                                bitmap?.let { runDetection(it) }
                            }
                        }
                    }
                }
                else -> showToast("sorry ${result.resultCode}")
            }
        }

    private fun runDetection(bitmap: Bitmap) {
        val finalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val image = InputImage.fromBitmap(finalBitmap, 0)
        Log.e("img","$image")
        faceDetector?.process(image)
            ?.addOnFailureListener { error: Exception ->
                error.printStackTrace()
                mBinding.txtFaceCount.showSnackBar(error.toString())
            }
            ?.addOnSuccessListener { faces: List<Face> ->
                when {
                    faces.isEmpty() -> {
                        mBinding.txtFaceCount.text = "Face Count = 0"
                    }
                    else -> {
                        Log.e("faces detected", "${faces.size}")
                        mBinding.txtFaceCount.text = "Face Count = ${faces.size}"
                    }
                }
            }
            .run {    mBinding.txtFaceCount.text = "Face Count = 0"}
    }

    fun ImageFromCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "New Picture")
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera")

        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        filePhoto = getPhotoFile(FILE_NAME)
        val providerFile = FileProvider.getUriForFile(this,"com.ebinumer.headcount.fileprovider", filePhoto)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)


        try {
            resultLauncher.launch(takePictureIntent)
            isCamera = true

        } catch (e: ActivityNotFoundException) {
            mBinding.btnCamera.showSnackBar(e.toString())
        }
    }

    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }
    //TODO takes URI of the image and returns bitmap
    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
}