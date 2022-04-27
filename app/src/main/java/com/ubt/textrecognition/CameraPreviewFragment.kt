package com.ubt.textrecognition

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ubt.textrecognition.databinding.FragmentCameraPreviewBinding

class CameraPreviewFragment : CameraXFragment<FragmentCameraPreviewBinding>() {

    override val cameraId: String by lazy {
        arguments?.getString("cameraId") ?: "0"
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentCameraPreviewBinding {
        return FragmentCameraPreviewBinding.inflate(inflater, container, false)
    }

    override fun getSurfaceView(): BaseSurfaceView = binding.viewfinder

    companion object {

        fun newInstance(cameraId: String) =
            CameraPreviewFragment().apply {
                arguments = Bundle().apply {
                    putString("cameraId", cameraId)
                }
            }
    }
}