package com.ubt.textrecognition.render

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import com.ubt.textrecognition.BaseSurfaceView
import com.ubt.textrecognition.OnTextureCreated

class CameraSurfaceView : BaseSurfaceView, CameraRenderer.OnViewSizeAvailableListener {

    private val renderer: CameraRenderer

    init {
        setEGLContextClientVersion(2)
        renderer = CameraRenderer(context, this)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun getViewSize(): Size = Size(width, height)

    override fun setOnSurfaceCreated(callback: OnTextureCreated) {
        renderer.setOnSurfaceCreated(callback)
    }

}