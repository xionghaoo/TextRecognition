package com.ubt.textrecognition

import android.content.Context
import android.graphics.*
import android.util.Log
import android.widget.Toast
import com.ubt.textrecognition.widgets.IndicatorRectView
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import timber.log.Timber
import java.io.IOException
import kotlin.math.abs
import kotlin.math.roundToInt


class TrackCodeDetector(private val context: Context) {

    companion object {
        private const val OUTPUT_IMAGE_WIDTH = 360.0
        private const val OUTPUT_IMAGE_HEIGHT = 640.0
    }

    private lateinit var srcMat: Mat
    private var resultBitmap: Bitmap? = null
    private lateinit var contours2f: MatOfPoint2f
    private lateinit var approxCurve: MatOfPoint2f

    private val density = context.resources.displayMetrics.density

    /**
     * 行程码区域检测
     * 检测到会返回行程文字区域的图片
     */
    fun detect(image: Bitmap): Bitmap? {
        val srcWidth = image.width
        val srcHeight = image.height
        Timber.d("src size: ${srcWidth}, $srcHeight")

        val contour = findOuterArea(image) ?: return null
        // 多边形拟合，寻找区域顶点
        contours2f = MatOfPoint2f(*contour.toArray())
        // 近似精度的参数，值越小精度越高
        var epsilon = 0.03 * Imgproc.arcLength(contours2f, true)
        if (epsilon <= 5) {
            epsilon = 5.0
        }
        Timber.d("epsilon: ${epsilon}")
        approxCurve = MatOfPoint2f()
        // 拟合后的顶点集合approxCurve
        Imgproc.approxPolyDP(contours2f, approxCurve, epsilon, true)

        Timber.d("顶点数：${approxCurve.rows()}")
        // 过滤四个顶点的矩形
        val num = approxCurve.rows()
        if (num != 4) {
            Timber.d("识别失败：顶点数：$num")
            return null
        }

        var p1 = Point(0.0, 0.0)
        var p2 = Point(0.0, 0.0)
        var p3 = Point(0.0, 0.0)
        var p4 = Point(0.0, 0.0)
        val ps = approxCurve.toList()
        ps.sortBy { p -> p.x }
        if (ps[0].y < ps[1].y) {
            p1 = ps[0]
            p2 = ps[1]
        } else {
            p1 = ps[1]
            p2 = ps[0]
        }
        if (ps[2].y < ps[3].y) {
            p3 = ps[2]
            p4 = ps[3]
        } else {
            p3 = ps[3]
            p4 = ps[2]
        }
        Timber.d("边界：${p1}, ${p2}, ${p3}, ${p4}")

        // 透视变换矫正
        val srcPoints = ArrayList<Point>()
        srcPoints.add(p1)
        srcPoints.add(p3)
        srcPoints.add(p2)
        srcPoints.add(p4)

        val targetWidth = OUTPUT_IMAGE_WIDTH * density
        val targetHeight = OUTPUT_IMAGE_HEIGHT * density
        val pts1 = Converters.vector_Point2f_to_Mat(srcPoints)
        val dstPoints = ArrayList<Point>()
        dstPoints.add(Point(0.0, 0.0))
        dstPoints.add(Point(targetWidth, 0.0))
        dstPoints.add(Point(0.0, targetHeight))
        dstPoints.add(Point(targetWidth, targetHeight))
        val pts2 = Converters.vector_Point2f_to_Mat(dstPoints)
        val transform = Imgproc.getPerspectiveTransform(pts1, pts2)
        val dstMat = Mat()
        Imgproc.warpPerspective(srcMat, dstMat, transform, Size(targetWidth, targetHeight), Imgproc.INTER_NEAREST)

        resultBitmap = matToBitmap(dstMat)
        // 透视变换后处理，寻找裁剪区域
        val cropRect = findCropRect(dstMat) ?: return null
        // 图片裁剪
        val result = Bitmap.createBitmap(
            resultBitmap!!,
            cropRect.x,
            cropRect.y + cropRect.height / 2,
            cropRect.width,
            cropRect.height / 2
        )
        return result
    }

    /**
     * 寻找外围轮廓
     */
    private fun findOuterArea(image: Bitmap): MatOfPoint? {
        srcMat = Mat()
        val hsvMat = Mat()
        try {
            Utils.bitmapToMat(image, srcMat)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        // hsv空间颜色过滤
        Imgproc.cvtColor(srcMat, hsvMat, Imgproc.COLOR_BGR2HSV)
        val binaryMat = Mat()
        // 行程码绿色值过滤
        Core.inRange(hsvMat, Scalar(35.0, 60.0, 60.0), Scalar(85.0, 255.0, 255.0), binaryMat)

        // 寻找轮廓
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
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
//        resultBitmap = matToBitmap(binaryMat)
//        drawContours(binaryMat, cs)
        return cs.maxByOrNull { Imgproc.boundingRect(it).area() }
    }

    /**
     * 寻找轮廓中的裁剪区域
     */
    private fun findCropRect(transformMat: Mat): Rect? {
        val transformImage = matToBitmap(transformMat)
        val tranSrcMat = Mat()
        try {
            Utils.bitmapToMat(transformImage, tranSrcMat)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val tranGreyMat = Mat()
        Imgproc.cvtColor(tranSrcMat, tranGreyMat, Imgproc.COLOR_BGR2GRAY)
        val tranBinaryMat = Mat()
        Imgproc.threshold(tranGreyMat, tranBinaryMat, 180.0, 255.0, Imgproc.THRESH_BINARY)

        val tranHierarchy = Mat()
        val tranContours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            tranBinaryMat,
            tranContours,
            tranHierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        val filtedContours = tranContours.filter { contour ->
            Imgproc.contourArea(contour) > 2500
        }
        val maxContour = filtedContours.maxByOrNull { Imgproc.contourArea(it) } ?: return null
        val rect = Imgproc.boundingRect(maxContour)
        return rect
    }


    /**
     * 边缘检测
     */
    private fun cannyDetect(image: Bitmap): MatOfPoint? {
        srcMat = Mat()
        val targetMat = Mat()
        val result = Mat()
        try {
            Utils.bitmapToMat(image, srcMat)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Imgproc.GaussianBlur(srcMat, targetMat, Size(5.0, 5.0), 5.0)
        Imgproc.Canny(targetMat, result, 120.0, 240.0, 3)

//        val binaryMat = Mat()
        val hierarchy = Mat()
        val contours = ArrayList<MatOfPoint>()
        Imgproc.findContours(
            result,
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
        return cs.maxByOrNull { Imgproc.boundingRect(it).area() }
    }

    private fun findInnerArea(image: Bitmap, rect: Rect) {
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
        Imgproc.threshold(newGreyMat, newBinaryMat, 180.0, 255.0, Imgproc.THRESH_BINARY)

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

//        val maxContour = filtedContours.maxByOrNull { Imgproc.boundingRect(it).area() }
//        val maxRect = Imgproc.boundingRect(maxContour)
//
//        val left = maxRect.x
//        val top = maxRect.y
//        val bottom = top + maxRect.height
//        val right = left + maxRect.width
//        val r = android.graphics.Rect(left, top, right, bottom)
//
//        val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//            strokeWidth = 2f
//            style = Paint.Style.STROKE
//            color = Color.argb(255, 255, 0, 0)
//        }
//        canvas.drawRect(r, rectPaint)
//        maxContour?.toList()?.forEach { p ->
//            Timber.d("contour point: $p")
//        }

        this.resultBitmap = resultBitmap
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val tmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, tmp)
        return tmp
    }

    private fun drawContours(mat: Mat, contours: List<MatOfPoint>) {
        val bitmap = matToBitmap(mat)
        val canvas = Canvas(bitmap)
        contours.forEachIndexed { index, matOfPoint ->
            val rect = Imgproc.boundingRect(matOfPoint)
            Timber.d("轮廓$index: ${rect.width}, ${rect.height}, (${rect.x}, ${rect.y})")

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
    }
}