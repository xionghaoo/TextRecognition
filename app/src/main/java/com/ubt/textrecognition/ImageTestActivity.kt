package com.ubt.textrecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import com.googlecode.tesseract.android.TessBaseAPI
import com.ubt.textrecognition.databinding.ActivityImageTestBinding
import timber.log.Timber
import java.io.File

class ImageTestActivity : AppCompatActivity() {

    // 图像分析
    private val tess = TessBaseAPI()

    private val imageDetector = ImageDetector()

    private lateinit var binding: ActivityImageTestBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 文字模型路径/sdcard/tesseract/tessdata/
        val dataPath: String = File(Environment.getExternalStorageDirectory(), "tesseract").absolutePath
        if (tess.init(dataPath, "chi_sim")) {
            Timber.d("Tesseract引擎初始化成功")
        }

        val testImg = R.drawable.test3
        binding.ivImage.setImageResource(testImg)
        Thread {
            val bitmap = BitmapFactory.decodeResource(resources, testImg)
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