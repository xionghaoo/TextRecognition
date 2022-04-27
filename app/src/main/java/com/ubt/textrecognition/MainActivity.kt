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
import xh.zero.core.utils.SystemUtil

class MainActivity : AppCompatActivity(), CameraXFragment.OnFragmentActionListener {

    private lateinit var binding: ActivityMainBinding
    private var isInit = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionTask()
    }

    private fun showCameraView(id: String) {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            if (isInit) {
                isInit = false
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
                    val characteristic = cameraManager.getCameraCharacteristics(cameraId)
                    if (index == 0) {
                        characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            ?.getOutputSizes(ImageFormat.JPEG)
                            ?.maxByOrNull { it.width * it.height }
                            ?.also { maxImageSize ->
                                // Nexus6P相机支持的最大尺寸：4032x3024
                                Timber.d("相机支持的最大尺寸：${maxImageSize}")
                                val metrics = WindowManager(this).getCurrentWindowMetrics().bounds
                                // Nexus6P屏幕尺寸：1440 x 2560，包含NavigationBar的高度
                                Timber.d("屏幕尺寸：${metrics.width()} x ${metrics.height()}")
                                val lp = binding.fragmentContainer.layoutParams as FrameLayout.LayoutParams

                                Timber.d("屏幕方向: ${if (resources.configuration.orientation == 1) "竖直" else "水平"}")
                                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    // 竖直方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                                    val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                                    lp.width = metrics.width()
                                    // Nexus6P 竖直方向屏幕计算高度
                                    // 等比例关系：1440 / height = 3024 / 4032
                                    // height = 4032 / 3024 * 1440
                                    lp.height = (metrics.width() / ratio).toInt()
                                } else {
                                    // 水平方向：设置预览区域的尺寸，这个尺寸用于接收SurfaceTexture的显示
                                    val ratio = maxImageSize.height.toFloat() / maxImageSize.width.toFloat()
                                    // Nexus6P 竖直方向屏幕计算高度
                                    // 等比例关系：width / 1440 = 4032 / 3024
                                    // width = 4032 / 3024 * 1440
                                    lp.width = (metrics.height() / ratio).toInt()
                                    lp.height = metrics.height()
                                    Timber.d("相机预览视图尺寸：${lp.width} x ${lp.height}")
                                }
                                lp.gravity = Gravity.CENTER

                                replaceFragment(CameraPreviewFragment.newInstance(id), R.id.fragment_container)
                            }

                        val cameraOrientation = characteristic.get(CameraCharacteristics.SENSOR_ORIENTATION)
                        Timber.d("摄像头角度：$cameraOrientation")
                    }
                }
            }
        }

    }

    override fun showAnalysisResult(result: String) {
        runOnUiThread {
            binding.ivImage.text = result
        }
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

            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraManager.cameraIdList.forEachIndexed { index, cameraId ->
                val characteristic = cameraManager.getCameraCharacteristics(cameraId)
                if (index == 0) {
                    val configurationMap = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val sizeList = configurationMap?.getOutputSizes(ImageFormat.JPEG)
                    // 相机支持的尺寸
                    sizeList?.forEach { size ->
                        Timber.d("camera support: [${size.width}, ${size.height}]")
                    }
                    showCameraView(cameraId)
                }
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