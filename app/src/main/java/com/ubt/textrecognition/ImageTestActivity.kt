package com.ubt.textrecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import com.googlecode.tesseract.android.TessBaseAPI
import com.ubt.textrecognition.databinding.ActivityImageTestBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import xh.zero.core.utils.SystemUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception

class ImageTestActivity : AppCompatActivity() {

    // 图像分析
    private val tess = TessBaseAPI()

    private val imageDetector by lazy {
        TrackCodeDetector(this)
    }

    private lateinit var binding: ActivityImageTestBinding
    private var num: Int = 0
    private val webSocketClient = WebSocketClient { r ->
        binding.tvResult.text = r.toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityImageTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 文字模型路径/sdcard/tesseract/tessdata/
//        val dataPath: String = File(Environment.getExternalStorageDirectory(), "tesseract").absolutePath
//        if (tess.init(dataPath, "chi_sim")) {
//            Timber.d("Tesseract引擎初始化成功")
//        }

        val testImg = R.drawable.test_left
//        recognize(testImg)

        binding.ivImage.setOnClickListener {
            num ++
            if (num >= 6) num = 0
            val img = when (num) {
                0 -> R.drawable.test_left
                1 -> R.drawable.test_right
                2 -> R.drawable.test_top
                3 -> R.drawable.test_bottom
                4 -> R.drawable.test
                else -> R.drawable.test2
            }
            recognize(img)
        }

        webSocketClient.start {
            recognize(testImg)
        }
    }

//    private fun recognize(img: Int) {
//        binding.ivImage.setImageResource(img)
//
//        Thread {
//            try {
//                val bitmap = BitmapFactory.decodeResource(resources, img)
//                val result = imageDetector.detect(bitmap)
//                tess.setImage(result)
//                val txt = tess.utF8Text
//                Timber.d("识别结果：${txt}")
//                runOnUiThread {
//                    binding.ivResult.setImageBitmap(result)
//                    binding.tvResult.text = txt
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }.start()
//    }

    private fun recognize(img: Int) {
        binding.ivImage.setImageResource(img)

        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = BitmapFactory.decodeResource(resources, img)
            val result = imageDetector.detect(bitmap)
            webSocketClient.send(encodeImage(result!!))
            withContext(Dispatchers.Main) {
                binding.ivResult.setImageBitmap(result)
            }
        }

    }

    private fun encodeImage(bm: Bitmap): String? {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }

    override fun onDestroy() {
        tess.recycle()
        super.onDestroy()
    }
}