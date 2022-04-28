package com.ubt.textrecognition

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import com.googlecode.tesseract.android.TessBaseAPI
import com.ubt.textrecognition.databinding.ActivityImageTestBinding
import timber.log.Timber
import xh.zero.core.utils.SystemUtil
import java.io.File

class ImageTestActivity : AppCompatActivity() {

    // 图像分析
    private val tess = TessBaseAPI()

    private val imageDetector by lazy {
        TrackCodeDetector(this)
    }

    private lateinit var binding: ActivityImageTestBinding
    private var num: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemUtil.toFullScreenMode(this)
        binding = ActivityImageTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 文字模型路径/sdcard/tesseract/tessdata/
        val dataPath: String = File(Environment.getExternalStorageDirectory(), "tesseract").absolutePath
        if (tess.init(dataPath, "chi_sim")) {
            Timber.d("Tesseract引擎初始化成功")
        }

        val testImg = R.drawable.test_left
        recognize(testImg)

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

    }

    private fun recognize(img: Int) {
        binding.ivImage.setImageResource(img)

        Thread {
            val bitmap = BitmapFactory.decodeResource(resources, img)
            val result = imageDetector.detect(bitmap)
            tess.setImage(result)
            val txt = tess.utF8Text
            Timber.d("识别结果：${txt}")
            runOnUiThread {
                binding.ivResult.setImageBitmap(result)
                binding.tvResult.text = txt
            }
        }.start()
    }

    override fun onDestroy() {
        tess.recycle()
        super.onDestroy()
    }
}