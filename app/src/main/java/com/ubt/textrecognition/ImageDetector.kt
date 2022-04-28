package com.ubt.textrecognition

import android.content.Context
import android.graphics.*
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


class ImageDetector {
    private lateinit var srcMat: Mat
    private var resultBitmap: Bitmap? = null
    private lateinit var contours2f: MatOfPoint2f
    private lateinit var approxCurve: MatOfPoint2f


    fun detect(image: Bitmap): Bitmap? {
        val srcWidth = image.width
        val srcHeight = image.height
        Timber.d("src size: ${srcWidth}, $srcHeight")

//        val contour = cannyDetect(image) ?: return null

        val contour = findOuterArea(image) ?: return null
        val rect = Imgproc.boundingRect(contour)
        val ps = Array<Point>(4) { Point(0.0, 0.0) }
        contour.toList()?.forEach { point ->
            if (abs(point.y.toInt() - rect.y) < 3) {
                ps[0] = point
            }
            if (abs(point.x - (rect.x + rect.width)) < 3) {
                ps[2] = point
            }

            if (abs(point.x.toInt() - rect.x) < 3) {
                ps[1] = point
            }

            if (abs(point.y - (rect.y + rect.height)) < 3) {
                ps[3] = point
            }
        }

        contours2f = MatOfPoint2f(*contour.toArray())
//        val rectf = Imgproc.minAreaRect(contours2f)
//        val ps = Array<Point>(4) { Point(0.0, 0.0) }
//        rectf.points(ps)
//        Timber.d("最小矩形：${ps[0]}, ${ps[1]}, ${ps[2]}, ${ps[3]}")

        // 靠左倾斜角度：82.56858825683594
        // 靠右倾斜角度：2.1522600650787354
//        Timber.d("角度：${rectf.angle}")

        Timber.d("边界矩形：$rect")

        var p1 = Point(0.0, 0.0)
        var p2 = Point(0.0, 0.0)
        var p3 = Point(0.0, 0.0)
        var p4 = Point(0.0, 0.0)

        // 坐标点排序
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

        val srcPoints = ArrayList<Point>()
        srcPoints.add(p1)
        srcPoints.add(p3)
        srcPoints.add(p2)
        srcPoints.add(p4)

        val pts1 = Converters.vector_Point2f_to_Mat(srcPoints)
        val dstPoints = ArrayList<Point>()
        dstPoints.add(Point(0.0, 0.0))
        dstPoints.add(Point(rect.width.toDouble(), 0.0))
        dstPoints.add(Point(0.0, rect.height.toDouble()))
        dstPoints.add(Point(rect.width.toDouble(), rect.height.toDouble()))
        val pts2 = Converters.vector_Point2f_to_Mat(dstPoints)
        val transform = Imgproc.getPerspectiveTransform(pts1, pts2)
        val dstMat = Mat()
        Imgproc.warpPerspective(srcMat, dstMat, transform, Size(rect.width.toDouble(), rect.height.toDouble()), Imgproc.INTER_NEAREST)

        resultBitmap = matToBitmap(dstMat)

        // 图片裁剪
        val r = rect.height / 209.0
        val newBitmap = Bitmap.createBitmap(resultBitmap!!, 0, (r * 130).roundToInt(), rect.width, (r * 21).roundToInt())

//        findInnerArea(image, outerRect)
        // TODO 1. 找到内部最大轮廓的顶点
        // TODO 2. 透视变换
        return newBitmap
    }

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
        Core.inRange(hsvMat, Scalar(35.0, 60.0, 60.0), Scalar(80.0, 255.0, 255.0), binaryMat)

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
//        val canvas = Canvas(resultBitmap!!)
//        cs.forEachIndexed { index, matOfPoint ->
//            val rect = Imgproc.boundingRect(matOfPoint)
//            Timber.d("轮廓$index: ${rect.width}, ${rect.height}, (${rect.x}, ${rect.y})")
//
//            val left = rect.x
//            val top = rect.y
//            val bottom = top + rect.height
//            val right = left + rect.width
//            val r = android.graphics.Rect(left, top, right, bottom)
//
//            val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//                strokeWidth = 2f
//                style = Paint.Style.STROKE
//                color = Color.argb(255, 255, 0, 0)
//            }
//            canvas.drawRect(r, rectPaint)
//        }


        return cs.maxByOrNull { Imgproc.boundingRect(it).area() }
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
//        resultBitmap = matToBitmap(result)
//        val canvas = Canvas(resultBitmap!!)
//        cs.forEachIndexed { index, matOfPoint ->
//            val rect = Imgproc.boundingRect(matOfPoint)
//            Timber.d("轮廓$index: ${rect.width}, ${rect.height}, (${rect.x}, ${rect.y})")
//
//            val left = rect.x
//            val top = rect.y
//            val bottom = top + rect.height
//            val right = left + rect.width
//            val r = android.graphics.Rect(left, top, right, bottom)
//
//            val rectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//                strokeWidth = 2f
//                style = Paint.Style.STROKE
//                color = Color.argb(255, 255, 0, 0)
//            }
//            canvas.drawRect(r, rectPaint)
//        }

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
}