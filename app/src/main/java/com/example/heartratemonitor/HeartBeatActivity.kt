package com.example.heartratemonitor

import android.annotation.SuppressLint
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import java.util.concurrent.atomic.AtomicBoolean

class HeartBeatActivity : AppCompatActivity() {

    private val TAG = "HeartRateMonitor"
    private var processing = AtomicBoolean(false)
    private var previewHolder: SurfaceHolder? = null
    private var camera: Camera? = null

    private var preview: SurfaceView? = null
    private var title: TextView? = null
    private var description: TextView? = null
    private var beatIndicator: ImageView? = null
    private var beatTv: TextView? = null
    private var isStopped = false

    private var wakeLock: WakeLock? = null

    private var averageIndex = 0
    private var averageArraySize = 4
    private var averageArray = IntArray(averageArraySize)

    enum class TYPE {
        GREEN, RED
    }

    private var currentType = TYPE.GREEN

    fun getCurrent(): TYPE {
        return currentType
    }

    private var beatsIndex = 0
    private var beatsArraySize = 3
    private var beatsArray = IntArray(beatsArraySize)
    private var beats = 0.0
    private var startTime: Long = 0
    private var calculatedBeats = mutableListOf<Int>()

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setTitle("CardiCare LightBeat ")
        supportActionBar?.hide()

        preview = findViewById(R.id.preview)
        title = findViewById(R.id.title)
        description = findViewById(R.id.description)
        beatIndicator = findViewById(R.id.beat_indicator)
        beatTv = findViewById(R.id.beat_tv)

        previewHolder = preview?.holder
        previewHolder?.addCallback(surfaceCallback)
        previewHolder?.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "DoNotDimScreen")
    }

    override fun onResume() {
        super.onResume()
        startCalculating()
    }

    override fun onPause() {
        super.onPause()
        if(isStopped.not()) stopCalculating()
    }

    private fun startCalculating() {
        wakeLock?.acquire()
        camera = Camera.open()
        startTime = System.currentTimeMillis()
        title?.postDelayed(62000) {
            stopCalculating()
        }
    }

    private fun stopCalculating() {
        isStopped = true
        wakeLock?.release()

        camera?.setPreviewCallback(null)
        camera?.stopPreview()
        camera?.release()
        camera = null
        calculateAverageBeat()
    }

    private fun calculateAverageBeat() {
        val finalRate = calculatedBeats.average().toInt().toString()
        beatTv?.text = finalRate
        Toast.makeText(this, "Your Heart Rate is: $finalRate", Toast.LENGTH_LONG).show()
//        beatTv?.text = calculatedBeats.toString()
    }

    private val previewCallback =
        PreviewCallback { data, cam ->

            /**
             * {@inheritDoc}
             */
            /**
             * {@inheritDoc}
             */
            if (data == null) throw NullPointerException()
            val size = cam.parameters.previewSize ?: throw NullPointerException()
            if (!processing.compareAndSet(
                    false,
                    true
                )
            ) return@PreviewCallback
            val width = size.width
            val height = size.height
            val imgAvg = ImageProcessing.decodeYUV420SPtoRedAvg(data.clone(), height, width)
            // Log.i(TAG, "imgAvg="+imgAvg);
            if (imgAvg == 0 || imgAvg == 255) {
                processing.set(false)
                return@PreviewCallback
            }
            var averageArrayAvg = 0
            var averageArrayCnt = 0
            for (i in averageArray.indices) {
                if (averageArray.get(i) > 0) {
                    averageArrayAvg += averageArray.get(
                        i
                    )
                    averageArrayCnt++
                }
            }
            val rollingAverage = if (averageArrayCnt > 0) averageArrayAvg / averageArrayCnt else 0
            var newType: TYPE = currentType
            if (imgAvg < rollingAverage) {
                newType = TYPE.RED
                if (newType != currentType) {
                    beats++
                    // Log.d(TAG, "BEAT!! beats="+beats);
                }
            } else if (imgAvg > rollingAverage) {
                newType = TYPE.GREEN
            }
            if (averageIndex == averageArraySize) averageIndex =
                0
            averageArray[averageIndex] = imgAvg
            averageIndex++

            // Transitioned from one state to another to the same
            if (newType != currentType) {
                currentType = newType
                beatIndicator?.setImageResource(getImageForHeartBeat(currentType))
//                image?.text = currentType.name // TODO: luplup karao
            }
            val endTime = System.currentTimeMillis()
            val totalTimeInSecs: Double =
                (endTime - startTime) / 1000.0
            if (totalTimeInSecs >= 10) {
                val bps: Double = beats / totalTimeInSecs
                val dpm = (bps * 60.0).toInt()
                if (dpm < 30 || dpm > 180) {
                    startTime = System.currentTimeMillis()
                    beats = 0.0
                    processing.set(false)
                    return@PreviewCallback
                }

                // Log.d(TAG,
                // "totalTimeInSecs="+totalTimeInSecs+" beats="+beats);
                if (beatsIndex == beatsArraySize) beatsIndex = 0
                beatsArray[beatsIndex] = dpm
                beatsIndex++
                var beatsArrayAvg = 0
                var beatsArrayCnt = 0
                for (i in beatsArray.indices) {
                    if (beatsArray.get(i) > 0) {
                        beatsArrayAvg += beatsArray.get(i)
                        beatsArrayCnt++
                    }
                }
                val beatsAvg = beatsArrayAvg / beatsArrayCnt
                beatTv?.setText(beatsAvg.toString())
                calculatedBeats.add(beatsAvg)
                startTime = System.currentTimeMillis()
                beats = 0.0
            }
            processing.set(false)
        }

    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        /**
         * {@inheritDoc}
         */
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera?.setPreviewDisplay(previewHolder)
                camera?.setPreviewCallback(previewCallback)
            } catch (t: Throwable) {
                Log.e("PreviewDemo-surfaceCallback", "Exception in setPreviewDisplay()", t)
            }
        }

        /**
         * {@inheritDoc}
         */
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val parameters: Camera.Parameters? = camera?.getParameters()
            parameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            val size = getSmallestPreviewSize(width, height, parameters)
            if (size != null) {
                parameters?.setPreviewSize(size.width, size.height)
                Log.d(
                    TAG,
                    "Using width=" + size.width + " height=" + size.height
                )
            }
            camera?.setParameters(parameters)
            camera?.startPreview()
        }

        /**
         * {@inheritDoc}
         */
        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // Ignore
        }
    }

    private fun getSmallestPreviewSize(
        width: Int,
        height: Int,
        parameters: Camera.Parameters?
    ): Camera.Size? {
        var result: Camera.Size? = null
        for (size in parameters?.supportedPreviewSizes.orEmpty()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size
                } else {
                    val resultArea = result.width * result.height
                    val newArea = size.width * size.height
                    if (newArea < resultArea) result = size
                }
            }
        }
        return result
    }

    private fun getImageForHeartBeat(type: TYPE): Int {
        return when(type) {
            TYPE.GREEN -> R.drawable.green_heart
            else -> R.drawable.red_heart
        }
    }
}