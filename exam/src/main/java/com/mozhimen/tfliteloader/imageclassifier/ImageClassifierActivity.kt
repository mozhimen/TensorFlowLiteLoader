package com.mozhimen.tfliteloader.imageclassifier

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.camera.core.ImageProxy
import com.mozhimen.basick.elemk.androidx.appcompat.bases.databinding.BaseActivityVB
import com.mozhimen.basick.lintk.optins.permission.OPermission_CAMERA
import com.mozhimen.basick.manifestk.cons.CPermission
import com.mozhimen.basick.manifestk.permission.ManifestKPermission
import com.mozhimen.basick.manifestk.permission.annors.APermissionCheck
import com.mozhimen.basick.utilk.android.app.UtilKLaunchActivity
import com.mozhimen.basick.utilk.android.graphics.applyBitmapAnyRotate
import com.mozhimen.camerak.camerax.annors.ACameraKXFacing
import com.mozhimen.camerak.camerax.annors.ACameraKXFormat
import com.mozhimen.camerak.camerax.commons.ICameraXKFrameListener
import com.mozhimen.camerak.camerax.mos.CameraKXConfig
import com.mozhimen.camerak.camerax.utils.imageProxyRgba88882bitmapRgba8888
import com.mozhimen.camerak.camerax.utils.imageProxyYuv4208882bitmapJpeg
import com.mozhimen.classifyloader.tflite.TFLiteImageClassifier
import com.mozhimen.tfliteloader.databinding.ActivityImageClassifierBinding
import java.util.concurrent.locks.ReentrantLock

@APermissionCheck(CPermission.CAMERA)
class ImageClassifierActivity : BaseActivityVB<ActivityImageClassifierBinding>() {
    private lateinit var _tFLiteImageClassifier: TFLiteImageClassifier
//    private lateinit var _tFLiteLabelImageClassifier: TFLiteLabelImageClassifier
//    private lateinit var _tFImageClassifier: TFImageClassifier

    override fun initData(savedInstanceState: Bundle?) {

        ManifestKPermission.requestPermissions(this, onResult = {
            if (it) {
                super.initData(savedInstanceState)
            } else {
                UtilKLaunchActivity.startSettingAppDetails(this)
            }
        })
    }

    override fun initView(savedInstanceState: Bundle?) {
        initLiteLoader()
        initCamera()
    }

    private fun initLiteLoader() {
        _tFLiteImageClassifier = TFLiteImageClassifier.create("health_model.tflite", resultSize = 3)
//        _tFLiteLabelImageClassifier = TFLiteLabelImageClassifier.create("?", "labels.txt", modelType = ModelType.QUANTIZED_EFFICIENTNET)
//        _tFImageClassifier = TFImageClassifier.create("output_graph.pb", "output_labels.txt", "input", 299, "output", 128f, 128f, 0.1f, 1)
    }

    @OptIn(OPermission_CAMERA::class)
    private fun initCamera() {
        vb.imageClassifierPreview.apply {
            initCameraKX(this@ImageClassifierActivity, CameraKXConfig(_format, ACameraKXFacing.BACK))
            setCameraXFrameListener(_cameraKXFrameListener)
        }
    }

    private var _outputBitmap: Bitmap? = null

    private val _format = ACameraKXFormat.YUV_420_888

    private val _cameraKXFrameListener: ICameraXKFrameListener by lazy {
        object : ICameraXKFrameListener {
            private val _reentrantLock = ReentrantLock()
            private val _stringBuilder = StringBuilder()

            @SuppressLint("UnsafeOptInUsageError", "SetTextI18n")
            override fun invoke(imageProxy: ImageProxy) {
                try {
                    _reentrantLock.lock()
                    when (_format) {
                        ACameraKXFormat.RGBA_8888 -> _outputBitmap = imageProxy.imageProxyRgba88882bitmapRgba8888()
                        ACameraKXFormat.YUV_420_888 -> _outputBitmap = imageProxy.imageProxyYuv4208882bitmapJpeg()
                    }
                    if (_outputBitmap != null) {
                        val rotateBitmap = _outputBitmap!!.applyBitmapAnyRotate(90f)

                        val objList = _tFLiteImageClassifier.classify(rotateBitmap, 0)
                        Log.d(TAG, "invoke: $objList")

                        runOnUiThread {
                            if (objList.isEmpty()) return@runOnUiThread
                            objList.forEachIndexed { index, _ ->
                                _stringBuilder.append("${objList[index].title}: ${objList[index].confidence}").append(" ")
                            }
                            vb.imageClassifierRes.text = _stringBuilder.toString()
                            _stringBuilder.clear()
                        }
                    }
                } finally {
                    _reentrantLock.unlock()
                }

                imageProxy.close()
            }
        }
    }
}