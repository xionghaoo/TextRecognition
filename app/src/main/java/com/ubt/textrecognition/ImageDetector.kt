package com.ubt.textrecognition

import android.content.Context
import android.graphics.*
import com.ubt.textrecognition.widgets.IndicatorRectView
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import java.io.IOException
import kotlin.math.roundToInt


class ImageDetector(private val context: Context) {

    private lateinit var contours: List<MatOfPoint>

    private lateinit var srcMat: Mat
    private lateinit var hsvMat: Mat
    private lateinit var greyMat: Mat
    private var resultBitmap: Bitmap? = null
    private lateinit var contours2f: MatOfPoint2f
    private lateinit var approxCurve: MatOfPoint2f


    fun detect(image: Bitmap): Bitmap? {
        val srcWidth = image.width
        val srcHeight = image.height
        Timber.d("src size: ${srcWidth}, $srcHeight")
        srcMat = Mat()
        hsvMat = Mat()
        greyMat = Mat()
        try {
            Utils.bitmapToMat(image, srcMat)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // hsv空间颜色过滤
        Imgproc.cvtColor(srcMat, hsvMat, Imgproc.COLOR_BGR2HSV)
        val binaryMat = Mat()
        Core.inRange(hsvMat, Scalar(50.0, 100.0, 100.0), Scalar(80.0, 255.0, 255.0), binaryMat)

        // 寻找轮廓
        val hierarchy = Mat()
        contours = ArrayList()
        Imgproc.findContours(
            binaryMat,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )
        Timber.d("找到轮廓数：${contours.size}")

        // 对找到的轮廓过滤
        val cs = contours.filter { contour ->
            Imgproc.contourArea(contour) > 2500
        }
        Timber.d("过滤后的轮廓：${cs.size}")
        val tmp = srcMat.clone()
        cs.forEachIndexed { index, matOfPoint ->
            val rect = Imgproc.boundingRect(matOfPoint)
            Timber.d("轮廓$index: ${rect.width}, ${rect.height}, (${rect.x}, ${rect.y})")
//            Imgproc.drawContours(tmp, cs, index, Scalar(156.0, 43.0, 46.0), 2, Imgproc.LINE_AA)
        }

        val rect = cs.map { c -> Imgproc.boundingRect(c) }
            .maxByOrNull { it.area() } ?: return null

        Timber.d("处理新的图片")
        val newSrcMat = Mat()
        val newGreyMat = Mat()
        val newBitmap = Bitmap.createBitmap(image, rect.x, rect.y, rect.width, rect.height)
        Timber.d("新的图片 size: ${newBitmap.width}, ${newBitmap.height}")

        try {
            Utils.bitmapToMat(newBitmap, newSrcMat)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        Imgproc.cvtColor(newSrcMat, newGreyMat, Imgproc.COLOR_BGR2GRAY)
        val newBinaryMat = Mat()
        Imgproc.threshold(newGreyMat, newBinaryMat, 200.0, 255.0, Imgproc.THRESH_BINARY)

        val newHierarchy = Mat()
        val newContours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            newBinaryMat,
            newContours,
            newHierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val filtedContours = newContours.filter { contour ->
            Imgproc.contourArea(contour) > 2500
        }
        filtedContours.forEachIndexed { index, contour ->
            val rect = Imgproc.boundingRect(contour)
            Timber.d("新轮廓$index: ${rect.width}, ${rect.height}, (${rect.x}, ${rect.y})")
        }

        val resultBitmap = matToBitmap(newBinaryMat).copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        filtedContours.forEach { contour ->
            val rect = Imgproc.boundingRect(contour)

            val left = rect.x
            val top = rect.y
            val bottom = top + rect.height
            val right = left + rect.width
            val r = android.graphics.Rect(left, top, right, bottom)

            val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 2f
                style = Paint.Style.STROKE
                color = Color.argb(255, 255, 0, 0)
            }
            canvas.drawRect(r, rectPaint)
        }



        return resultBitmap
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val tmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, tmp)
        return tmp
    }
}