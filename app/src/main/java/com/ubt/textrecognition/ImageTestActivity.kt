package com.ubt.textrecognition

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.ImageView
import android.widget.TextView
import com.googlecode.tesseract.android.TessBaseAPI
import timber.log.Timber
import java.io.File

class ImageTestActivity : AppCompatActivity() {

    // 图像分析
    private val tess = TessBaseAPI()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_test)

        // 文字模型路径/sdcard/tesseract/tessdata/
        val dataPath: String = File(Environment.getExternalStorageDirectory(), "tesseract").absolutePath
        if (tess.init(dataPath, "chi_sim")) {
            Timber.d("Tesseract引擎初始化成功")
        }

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test)
        val iv = findViewById<ImageView>(R.id.iv_image)
        val tv = findViewById<TextView>(R.id.tv_result)
        iv.setImageBitmap(bitmap)
        Thread {
            tess.setImage(bitmap)
            val txt = tess.utF8Text
            Timber.d("识别结果：${txt}")
            runOnUiThread {
                tv.text = txt
            }
        }.start()

    }
}