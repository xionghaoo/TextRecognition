//
// Created by xionghao on 2021/7/3.
//

#include <jni.h>
#include <string>
#include <iostream>
#include <opencv2/core.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/imgcodecs.hpp>
#include <android/log.h>
#include <android/bitmap.h>
#include <opencv2/opencv.hpp>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, "NativeLib", __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG , "NativeLib", __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO  , "NativeLib", __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN  , "NativeLib", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR  , "NativeLib", __VA_ARGS__)

using namespace std;
using namespace cv;

//void vector_Mat_to_Mat(std::vector<cv::Mat> &v_mat, cv::Mat &mat) {
//    int count = (int) v_mat.size();
//    mat.create(count, 1, CV_32SC2);
//    for (int i = 0; i < count; i++) {
//        long long addr = (long long) new Mat(v_mat[i]);
//        mat.at<Vec<int, 2> >(i, 0) = Vec<int, 2>(addr >> 32, addr & 0xffffffff);
//    }
//}

//void bitmap_to_mat(JNIEnv *env, jobject &srcBitmap, Mat &srcMat) {
//    void *srcPixels = 0;
//    AndroidBitmapInfo srcBitmapInfo;
//    try {
//        AndroidBitmap_getInfo(env, srcBitmap, &srcBitmapInfo);
//        AndroidBitmap_lockPixels(env, srcBitmap, &srcPixels);
//        uint32_t srcHeight = srcBitmapInfo.height;
//        uint32_t srcWidth = srcBitmapInfo.width;
//        srcMat.create(srcHeight, srcWidth, CV_8UC4);
//        if (srcBitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
//            Mat tmp(srcHeight, srcWidth, CV_8UC4, srcPixels);
//            tmp.copyTo(srcMat);
//        } else {
//            Mat tmp = Mat(srcHeight, srcWidth, CV_8UC2, srcPixels);
//            cvtColor(tmp, srcMat, COLOR_BGR5652RGBA);
//        }
//        AndroidBitmap_unlockPixels(env, srcBitmap);
//        return;
//    } catch (cv::Exception &e) {
//        AndroidBitmap_unlockPixels(env, srcBitmap);
//        jclass je = env->FindClass("java/lang/Exception");
//        env -> ThrowNew(je, e.what());
//        return;
//    } catch (...) {
//        AndroidBitmap_unlockPixels(env, srcBitmap);
//        jclass je = env->FindClass("java/lang/Exception");
//        env -> ThrowNew(je, "unknown");
//        return;
//    }
//}



