package com.demo.scannerapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException

class ScannerActivity : AppCompatActivity() {

    private lateinit var mContext: Context
    private val requestCodeCameraPermission = 1001
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""
    private lateinit var barcode_line: View
    private lateinit var cameraSurfaceView: SurfaceView

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)
        mContext = this
        try {
            barcode_line = findViewById<View>(R.id.barcode_line) as View
            cameraSurfaceView = findViewById<SurfaceView>(R.id.cameraSurfaceView) as SurfaceView

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                @Suppress("DEPRECATION") window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }

            if (ContextCompat.checkSelfPermission(
                    mContext, android.Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                askForCameraPermission()
            } else {
                setupControls()
            }

            val aniSlide: Animation =
                AnimationUtils.loadAnimation(mContext, R.animator.scanner_animation)
            barcode_line.startAnimation(aniSlide)
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }


    private fun setupControls() {
        try {
            barcodeDetector =
                BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()

            cameraSource =
                CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(1920, 1080)
                    .setAutoFocusEnabled(true) //you should add this feature
                    .build()

            cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
                @SuppressLint("MissingPermission")
                override fun surfaceCreated(holder: SurfaceHolder) {
                    try {
                        //Start preview after 1s delay
                        cameraSource.start(holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                @SuppressLint("MissingPermission")
                override fun surfaceChanged(
                    holder: SurfaceHolder, format: Int, width: Int, height: Int
                ) {
                    try {
                        cameraSource.start(holder)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    cameraSource.stop()
                }
            })

            barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
                override fun release() {
                    Toast.makeText(
                        applicationContext, "Scanner has been closed", Toast.LENGTH_SHORT
                    ).show()
                }

                override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                    val barcodes = detections.detectedItems
                    if (barcodes.size() == 1) {
                        scannedValue = barcodes.valueAt(0).rawValue
                        //Don't forget to add this line printing value or finishing activity must run on main thread
                        runOnUiThread {
                            cameraSource.stop()
                            Toast.makeText(
                                mContext, "Scanned Data :- \n$scannedValue", Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    } else {
                        Toast.makeText(mContext, "value- data not found!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            })
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    private fun askForCameraPermission() {
        try {
            ActivityCompat.requestPermissions(
                mContext as Activity,
                arrayOf(android.Manifest.permission.CAMERA),
                requestCodeCameraPermission
            )
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupControls()
                } else {
                    Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (exp: Exception) {
            exp.stackTrace
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.stop()
    }
}