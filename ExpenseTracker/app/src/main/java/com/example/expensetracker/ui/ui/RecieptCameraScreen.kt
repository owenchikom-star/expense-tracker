package com.example.expensetracker.ui.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun ReceiptCameraScreen(onTextRecognized: (String, String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    )
    { uri: android.net.Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            val path = saveBitmapToInternal(context, bitmap)
            runTextRecognition(bitmap) { text ->
                onTextRecognized(text, path)
            }
        }
    }
    
    
    Box {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    imageCapture = ImageCapture.Builder().build()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    imageCapture?.let { takePhotoAndScan(it, context, onTextRecognized) }
                },
            ) {
                Text("📸 Scan Receipt")
            }
            Button(
                onClick = {
                    galleryLauncher.launch("image/*")
                },
            ) {
                Text("📁 Gallery")
            }
        }
    }
}


fun takePhotoAndScan(
    imageCapture: ImageCapture,
    context: Context,
    onTextRecognized: (String, String) -> Unit
) {
    val receiptDir = File(context.filesDir, "receipts")
    receiptDir.mkdirs()
    val name = "Receipt_${System.currentTimeMillis()}.jpg"
    val outputFile = File(context.filesDir, "receipts/$name") // internal, always accessible
    outputFile.parentFile?.mkdirs()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

    imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                Log.d("RECEIPT", "Saved file to: ${outputFile.absolutePath}")
                val bitmap = BitmapFactory.decodeFile(outputFile.absolutePath)
                runTextRecognition(bitmap) { rawText ->
                    onTextRecognized(rawText, outputFile.absolutePath) // save file path, not URI
                }
            }
            override fun onError(ex: ImageCaptureException) {
                Log.e("Camera", "Photo capture failed", ex)
                onTextRecognized("", "")
            }
        })
}


private fun saveBitmapToInternal(context: Context, bitmap: Bitmap): String {
    val file = File(context.filesDir, "receipts/gallery_${System.currentTimeMillis()}.jpg")
    file.parentFile?.mkdirs()
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    return file.absolutePath
}

 private fun runTextRecognition(bitmap: Bitmap, onTextRecognized: (String) -> Unit) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            Log.d("RECEIPT", "OCR Result: ${visionText.text}")
            onTextRecognized(visionText.text)
        }
        .addOnFailureListener { e ->
            Log.e("OCR", "Failed", e)
            onTextRecognized("")
        }
}