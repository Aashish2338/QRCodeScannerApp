package com.demo.scannerapp

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var mContext: Context
    private val requestCodeCameraPermission = 1001
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var text_btn: TextView
    private lateinit var analyzer: MyImageAnalyzer

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this
        try {
            previewView = findViewById<PreviewView>(R.id.previewView) as PreviewView
            text_btn = findViewById<TextView>(R.id.text_btn) as TextView

            checkCameraPermission()

            text_btn.setOnClickListener {
                startActivity(Intent(mContext, ScannerActivity::class.java))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION")
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    private fun checkCameraPermission() {
        try {
            if (ContextCompat.checkSelfPermission(
                    mContext, CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                askForCameraPermission()
            } else {
                analyzer = MyImageAnalyzer(supportFragmentManager)

                cameraExecutor = Executors.newSingleThreadExecutor()
                cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)

                cameraProviderFuture.addListener(Runnable {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                }, ContextCompat.getMainExecutor(mContext))
            }
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    private fun askForCameraPermission() {
        try {
            ActivityCompat.requestPermissions(
                mContext as Activity,
                arrayOf(CAMERA),
                requestCodeCameraPermission
            )
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    analyzer = MyImageAnalyzer(supportFragmentManager)

                    cameraExecutor = Executors.newSingleThreadExecutor()
                    cameraProviderFuture = ProcessCameraProvider.getInstance(mContext)

                    cameraProviderFuture.addListener(Runnable {
                        val cameraProvider = cameraProviderFuture.get()
                        bindPreview(cameraProvider)
                    }, ContextCompat.getMainExecutor(mContext))
                } else {
                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        try {
            val preview: Preview = Preview.Builder()
                .build()
            val cameraSelector: CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
            preview.setSurfaceProvider(previewView.createSurfaceProvider(null))

            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                preview
            )

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

            cameraProvider.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector,
                imageAnalysis,
                preview
            )
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }
}