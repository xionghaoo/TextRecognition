package com.ubt.textrecognition

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import androidx.window.WindowManager
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * CameraX相机
 */
abstract class CameraXFragment<VIEW: ViewBinding> : Fragment() {

    protected lateinit var binding: VIEW

    protected abstract val cameraId: String

    private var displayId: Int = -1
    private var cameraProvider: ProcessCameraProvider? = null
//    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private lateinit var windowManager: WindowManager
    private var camera: Camera? = null

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var surfaceExecutor: ExecutorService

    // 照片输出路径
    private lateinit var outputDirectory: File
    private lateinit var surfaceTexture: SurfaceTexture

    private lateinit var bitmapBuffer: Bitmap
    private var imageRotationDegrees: Int = 0

    private var listener: OnFragmentActionListener? = null

    // 图像分析
//    private var tess = TessBaseAPI()
    private var isStopAnalysis = false
    private val trackCodeDetector: TrackCodeDetector by lazy {
        TrackCodeDetector(requireContext())
    }
    private val webSocketClient = WebSocketClient { r ->
//        binding.tvResult.text = r.toString()
        listener?.showAnalysisText(r.toString())
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentActionListener) {
            listener = context
        } else {
            throw IllegalArgumentException("Activity must implement OnFragmentActionListener")
        }
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    override fun onDestroy() {

        webSocketClient.stop()
//        tess.recycle()
        isStopAnalysis = true
        super.onDestroy()
    }

    private fun stopAnalysis() {
        // Terminate all outstanding analyzing jobs (if there is any).
        cameraExecutor.apply {
            shutdown()
            awaitTermination(1000, TimeUnit.MILLISECONDS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = getViewBinding(inflater, container)
        return binding.root
    }

    abstract fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): VIEW

    abstract fun getSurfaceView(): BaseSurfaceView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
        surfaceExecutor = Executors.newSingleThreadExecutor()
        // Determine the output directory
        outputDirectory = getOutputDirectory(requireContext())

        windowManager = WindowManager(view.context)

        getSurfaceView().setOnSurfaceCreated { sfTexture ->
            surfaceTexture = sfTexture
            surfaceTexture.setDefaultBufferSize(getSurfaceView().width, getSurfaceView().height)
            Timber.d("纹理缓冲区尺寸：${getSurfaceView().width} x ${getSurfaceView().height}")
            displayId = getSurfaceView().display.displayId
            webSocketClient.start {  }
            setupCamera()
        }

        // 文字模型路径/sdcard/tesseract/tessdata/
//        val dataPath: String = File(Environment.getExternalStorageDirectory(), "tesseract").absolutePath
//        if (tess.init(dataPath, "chi_sim")) {
//            Timber.d("Tesseract引擎初始化成功")
//        }

        // 手动对焦
//        getSurfaceView().setOnGestureDetect(object : GestureDetector.SimpleOnGestureListener() {
//            override fun onSingleTapUp(e: MotionEvent?): Boolean {
//                val x = e?.x ?: 0f
//                val y = e?.y ?: 0f
//
//                val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
//                    getSurfaceView().width.toFloat(), getSurfaceView().height.toFloat()
//                )
//
//                val autoFocusPoint = factory.createPoint(x, y)
//                try {
//                    camera?.cameraControl?.startFocusAndMetering(
//                        FocusMeteringAction.Builder(
//                            autoFocusPoint,
//                            FocusMeteringAction.FLAG_AF
//                        ).apply {
//                            //focus only when the user tap the preview
//                            disableAutoCancel()
//
//                            animFocusView(binding.root.findViewById(R.id.focus_view), x, y, true)
//                            animFocusView(binding.root.findViewById(R.id.focus_view_circle), x, y, false)
//                        }.build()
//                    )
//                } catch (e: CameraInfoUnavailableException) {
//                    Log.d("ERROR", "cannot access camera", e)
//                }
//                return true
//            }
//        })
    }

    /**
     * Focus view animation
     */
    private fun animFocusView(v: View, focusX: Float, focusY: Float, isRing: Boolean) {
        v.visibility = View.VISIBLE
        v.x = focusX - v.width / 2
        v.y = focusY - v.height / 2

        // 圆环和圆饼是不同的View，因此得到的ViewPropertyAnimator是不同的
        val anim = v.animate()
        anim.cancel()

        if (isRing) {
            // 圆环
            v.scaleX = 1.6f
            v.scaleY = 1.6f
            v.alpha = 1f
            anim.scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction {
                    v.animate()
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction { v.visibility = View.INVISIBLE }
                        .start()
                }
                .start()
        } else {
            // 圆饼
            v.scaleX = 0f
            v.scaleY = 0f
            v.alpha = 1f
            anim.scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .withEndAction {
                    v.animate()
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction { v.visibility = View.INVISIBLE }
                        .start()
                }
                .start()
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
//            lensFacing = when {
//                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
//                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
//                else -> throw IllegalStateException("Back and front camera are unavailable")
//            }
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    /**
     * 构建相机用例
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        // Get screen metrics used to setup camera for full screen resolution
        val metrics = windowManager.getCurrentWindowMetrics().bounds
        Timber.d("Screen metrics: ${metrics.width()} x ${metrics.height()}")

        val screenAspectRatio = aspectRatio(getSurfaceView().width, getSurfaceView().height)
        Timber.d("Preview aspect ratio: $screenAspectRatio")

        val rotation = getSurfaceView().display.rotation

        Timber.d("camera config rotation: $rotation")

        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder()
            // 开启前后置摄像头
//            .requireLensFacing(lensFacing)
            // 开启特定Id的摄像头
            .addCameraFilter { cameraList ->
                cameraList.filter { cameraInfo ->
                    Camera2CameraInfo.from(cameraInfo).cameraId == cameraId
                }
            }
            .build()

        // Preview 用例
        preview = Preview.Builder()
            // We request aspect ratio but no resolution
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation
            .setTargetRotation(rotation)
            .build()

        // ImageCapture 用例
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
            .setJpegQuality(100)
            .build()

        // ImageAnalysis 用例
        imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
                // 不能和setTargetResolution一起使用
//            .setTargetAspectRatio(screenAspectRatio)
            // Set initial target rotation, we will have to call this again if rotation changes
            // during the lifecycle of this use case
            .setTargetRotation(rotation)
                // 大分辨率
            .setTargetResolution(Size(960, 1280))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { image ->
                    if (!::bitmapBuffer.isInitialized) {
                        imageRotationDegrees = image.imageInfo.rotationDegrees
                        bitmapBuffer = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
                    }
//                    Timber.d("ImageAnalysis: image width: ${image.width}, height: ${image.height}, rotation: ${image.imageInfo.rotationDegrees}")

                    image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
                    // 拿到的图片是逆时针转了90度的图，这里修正它
                    val matrix = Matrix()
                    matrix.postRotate(90f)
                    val bitmap = Bitmap.createBitmap(bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height, matrix, true)
                    // 监听线程关闭的消息
                    if (isStopAnalysis) {
                        stopAnalysis()
                        return@Analyzer
                    }

                    try {
                        val result = trackCodeDetector.detect(bitmap)
                        if (result != null) {
//                            tess.setImage(result)
                            // 上面是一个耗时方法，不能直接把线程关掉，等他处理完再关掉
                            if (isStopAnalysis) {
                                stopAnalysis()
                            }
//                            val text: String = tess.utF8Text
//                            listener?.showAnalysisText(text)
                            CoroutineScope(Dispatchers.IO).launch {
                                webSocketClient.send(encodeImage(result))
                                listener?.showAnalysisResult(result)
                            }
                        } else {
                            listener?.showAnalysisText("")
                            listener?.showAnalysisResult(null)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                })
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer)

            // Attach the viewfinder's surface provider to preview use case
//            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            preview?.setSurfaceProvider { request ->
                val surface = Surface(surfaceTexture)
                request.provideSurface(surface, surfaceExecutor) { result ->
                    surface.release()
                    surfaceTexture.release()
                    // 0: success
                    Timber.d("surface used result: ${result.resultCode}")
                }
            }
            observeCameraState(camera?.cameraInfo!!)
        } catch (exc: Exception) {
            Timber.e("Use case binding failed: $exc")
        }
    }

    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun observeCameraState(cameraInfo: CameraInfo) {
        cameraInfo.cameraState.observe(viewLifecycleOwner) { cameraState ->
            run {
                when (cameraState.type) {
                    CameraState.Type.PENDING_OPEN -> {
                        // Ask the user to close other camera apps
                        Toast.makeText(context,
                            "CameraState: Pending Open",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.Type.OPENING -> {
                        // Show the Camera UI
                        Toast.makeText(context,
                            "CameraState: Opening",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.Type.OPEN -> {
                        // Setup Camera resources and begin processing
                        Toast.makeText(context,
                            "CameraState: Open",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.Type.CLOSING -> {
                        // Close camera UI
                        Toast.makeText(context,
                            "CameraState: Closing",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.Type.CLOSED -> {
                        // Free camera resources
                        Toast.makeText(context,
                            "CameraState: Closed",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }

            cameraState.error?.let { error ->
                when (error.code) {
                    // Open errors
                    CameraState.ERROR_STREAM_CONFIG -> {
                        // Make sure to setup the use cases properly
                        Toast.makeText(context,
                            "Stream config error",
                            Toast.LENGTH_SHORT).show()
                    }
                    // Opening errors
                    CameraState.ERROR_CAMERA_IN_USE -> {
                        // Close the camera or ask user to close another camera app that's using the
                        // camera
                        Toast.makeText(context,
                            "Camera in use",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                        // Close another open camera in the app, or ask the user to close another
                        // camera app that's using the camera
                        Toast.makeText(context,
                            "Max cameras in use",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {
                        Toast.makeText(context,
                            "Other recoverable error",
                            Toast.LENGTH_SHORT).show()
                    }
                    // Closing errors
                    CameraState.ERROR_CAMERA_DISABLED -> {
                        // Ask the user to enable the device's cameras
                        Toast.makeText(context,
                            "Camera disabled",
                            Toast.LENGTH_SHORT).show()
                    }
                    CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                        // Ask the user to reboot the device to restore camera function
                        Toast.makeText(context,
                            "Fatal error",
                            Toast.LENGTH_SHORT).show()
                    }
                    // Closed errors
                    CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                        // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
                        Toast.makeText(context,
                            "Do not disturb mode enabled",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * 拍照
     */
    fun takePhoto(complete: (path: String?) -> Unit) {

        // Get a stable reference of the modifiable image capture use case
        imageCapture?.let { imageCapture ->

            // Create output file to hold the image
            val photoFile = createFile(outputDirectory, FILENAME, PHOTO_EXTENSION)

            // Setup image capture metadata
            val metadata = ImageCapture.Metadata().apply {

                // Mirror image when using the front camera
//                isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
            }

            // Create output options object which contains file + metadata
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                .setMetadata(metadata)
                .build()

            // Setup image capture listener which is triggered after photo has been taken
            imageCapture.takePicture(
                outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                        requireActivity().runOnUiThread {
                            complete(savedUri.path)
                        }
                        Log.d(TAG, "Photo capture succeeded: ${savedUri.path}")

                        // We can only change the foreground Drawable using API level 23+ API
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Update the gallery thumbnail with latest picture taken
//                            setGalleryThumbnail(savedUri)
                        }

                        // Implicit broadcasts will be ignored for devices running API level >= 24
                        // so if you only target API level 24+ you can remove this statement
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                            requireActivity().sendBroadcast(
                                Intent(android.hardware.Camera.ACTION_NEW_PICTURE, savedUri)
                            )
                        }

                        // If the folder selected is an external media directory, this is
                        // unnecessary but otherwise other apps will not be able to access our
                        // images unless we scan them using [MediaScannerConnection]
                        val mimeType = MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(savedUri.toFile().extension)
                        MediaScannerConnection.scanFile(
                            context,
                            arrayOf(savedUri.toFile().absolutePath),
                            arrayOf(mimeType)
                        ) { _, uri ->
                            Log.d(TAG, "Image capture scanned into media store: $uri")
                        }
                    }
                })

            // We can only change the foreground Drawable using API level 23+ API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                // Display flash animation to indicate that photo was captured
//                fragmentCameraBinding.root.postDelayed({
//                    fragmentCameraBinding.root.foreground = ColorDrawable(Color.WHITE)
//                    fragmentCameraBinding.root.postDelayed(
//                        { fragmentCameraBinding.root.foreground = null }, ANIMATION_FAST_MILLIS)
//                }, ANIMATION_SLOW_MILLIS)
            }
        }
    }

    interface OnFragmentActionListener {
        fun showAnalysisResult(result: Bitmap?)
        fun showAnalysisText(txt: String)
    }

    companion object {
        private const val TAG = "CameraXFragment"
        private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val PHOTO_EXTENSION = ".jpg"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

        private const val FOCUS_AREA_RADIUS = 400

        /** Helper function used to create a timestamped file */
        private fun createFile(baseFolder: File, format: String, extension: String) =
            File(baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension)

        /** Use external media if it is available, our app's file directory otherwise */
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }
}