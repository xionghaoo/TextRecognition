package com.ubt.textrecognition

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
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

            binding.btnCameraRecognition.setOnClickListener {
                startPlainActivity(CameraActivity::class.java)
            }

            binding.btnImageRecognition.setOnClickListener {
                startPlainActivity(ImageTestActivity::class.java)
            }

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