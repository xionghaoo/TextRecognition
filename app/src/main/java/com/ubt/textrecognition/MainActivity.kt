package com.ubt.textrecognition

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.icu.util.Output
import android.os.Bundle
import android.os.Environment
import android.view.Gravity
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.window.WindowManager
import com.ubt.textrecognition.databinding.ActivityMainBinding
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import xh.zero.core.replaceFragment
import xh.zero.core.startPlainActivity
import xh.zero.core.utils.SystemUtil
import xh.zero.core.utils.ToastUtil
import java.io.*
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionTask()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(REQUEST_CODE_ALL_PERMISSION)
    private fun permissionTask() {
        if (hasPermission()) {
            Thread {
                // tesseract/tessdata/
                val fileName = "chi_sim.traineddata"
                val fis = assets.open(fileName)
                val tesseract = File(Environment.getExternalStorageDirectory(), "tesseract")
                if (!tesseract.exists()) {
                    tesseract.mkdir()
                }
                val tessdata = File(tesseract, "tessdata")
                if (!tessdata.exists()) {
                    tessdata.mkdir()
                }
                val targetFile = File(tessdata, fileName)
                if (!targetFile.exists()) {
                    targetFile.createNewFile()
                }
                var fout: FileOutputStream? = null
                try {
                    fout = FileOutputStream(targetFile)
                    val buffer = ByteArray(8 * 1024)
                    var read = fis.read(buffer)
                    while (read != -1) {
                        fout.write(buffer, 0, read)
                        read = fis.read(buffer)
                    }
                    fout.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    fout?.close()
                }
                runOnUiThread {
                    binding.btnCameraRecognition.setOnClickListener {
                        startPlainActivity(CameraActivity::class.java)
                    }

                    binding.btnImageRecognition.setOnClickListener {
                        startPlainActivity(ImageTestActivity::class.java)
                    }
                }
            }.start()




        } else {
            EasyPermissions.requestPermissions(
                this,
                "App需要相关权限，请授予",
                REQUEST_CODE_ALL_PERMISSION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    private fun hasPermission() : Boolean {
        return EasyPermissions.hasPermissions(
            this,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    companion object {
        private const val REQUEST_CODE_ALL_PERMISSION = 1

    }
}