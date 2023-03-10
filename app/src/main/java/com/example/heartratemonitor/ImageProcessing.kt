package com.example.heartratemonitor

import kotlin.math.roundToInt

object ImageProcessing {

    private fun decodeYUV420SPtoRedSum(yuv420sp: ByteArray?, width: Int, height: Int): Int {
//        if (yuv420sp == null) return 0
//        val frameSize = width * height
//        var sum = 0
//        var j = 0
//        var yp = 0
//        while (j < height) {
//            var uvp = frameSize + (j shr 1) * width
//            var u = 0
//            var v = 0
//            var i = 0
//            while (i < width) {
//                var y = (0xff and yuv420sp[yp].toInt()) - 16
//                if (y < 0) y = 0
//                if (i and 1 == 0) {
//                    v = (0xff and yuv420sp[uvp++].toInt()) - 128
//                    u = (0xff and yuv420sp[uvp++].toInt()) - 128
//                }
//                val y1192 = 1192 * y
//                var r = y1192 + 1634 * v
//                var g = y1192 - 833 * v - 400 * u
//                var b = y1192 + 2066 * u
//                if (r < 0) r = 0 else if (r > 262143) r = 262143
//                if (g < 0) g = 0 else if (g > 262143) g = 262143
//                if (b < 0) b = 0 else if (b > 262143) b = 262143
//                val pixel =
//                    -0x1000000 or (r shl 6 and 0xff0000) or (g shr 2 and 0xff00) or (b shr 10 and 0xff)
//                val red = pixel shr 16 and 0xff
//                sum += red
//                i++
//                yp++
//            }
//            j++
//        }
//        return sum
        val frameSize = width * height
        val rgb = IntArray(frameSize)
        var filteredData = DoubleArray(frameSize)
        val greenChannel = DoubleArray(frameSize)
        var sum = 0.0
        var count = 0

        // Step 1: Convert YUV420SP data to RGB format
        if (yuv420sp != null) {
            decodeYUV420SPtoRGB(rgb, yuv420sp, width, height)
        }

        // Step 2: Extract the green channel
        for (i in 0 until frameSize) {
            greenChannel[i] = (rgb[i] shr 8 and 0xff).toDouble() // extract green channel
        }

        // Step 3: Apply a bandpass filter
        val filter = BandpassFilter(40.0, 180.0, 25.0) // 40-180 bpm heart rate range
        filteredData = filter.apply(greenChannel)

        // Step 4: Detect peaks in the filtered signal
        val detector = PanTompkinsDetector(25.0)
        val peakIndices = detector.detect(filteredData)

        // Step 5: Calculate the heart rate
        for (i in 1 until peakIndices.size) {
            val timeDiff = (peakIndices[i] - peakIndices[i-1]) / 25.0 // 25 frames per second
            val heartRate = 60.0 / timeDiff
            sum += heartRate
            count++
        }
        return if (count > 0) (sum / count).roundToInt() else 0
    }

    private fun decodeYUV420SPtoRGB(rgb: IntArray, yuv420sp: ByteArray, width: Int, height: Int) {
        val frameSize = width * height
        var yp = 0
        for (j in 0 until height) {
            var uvp = frameSize + (j shr 1) * width
            var u = 0
            var v = 0
            for (i in 0 until width) {
                val y = (0xff and yuv420sp[yp].toInt())
                if (i and 1 == 0) {
                    v = (0xff and yuv420sp[uvp++].toInt()) - 128
                    u = (0xff and yuv420sp[uvp++].toInt()) - 128
                }
                val y1192 = 1192 * y
                var r = y1192 + 1634 * v
                var g = y1192 - 833 * v - 400 * u
                var b = y1192 + 2066 * u
                r = if (r < 0) 0 else if (r > 262143) 262143 else r
                g = if (g < 0) 0 else if (g > 262143) 262143 else g
                b = if (b < 0) 0 else if (b > 262143) 262143 else b
                rgb[yp++] = -0x1000000 or (r shr 10 and 0xff shl 16) or (g shr 10 and 0xff shl 8) or (b shr 10 and 0xff)
            }
        }
    }
    /**
     * Given a byte array representing a yuv420sp image, determine the average
     * amount of red in the image. Note: returns 0 if the byte array is NULL.
     *
     * @param yuv420sp
     * Byte array representing a yuv420sp image
     * @param width
     * Width of the image.
     * @param height
     * Height of the image.
     * @return int representing the average amount of red in the image.
     */
    fun decodeYUV420SPtoRedAvg(yuv420sp: ByteArray?, width: Int, height: Int): Int {
        if (yuv420sp == null) return 0
        val frameSize = width * height
        val sum = decodeYUV420SPtoRedSum(yuv420sp, width, height)
//        return sum / frameSize
        return sum
    }
}