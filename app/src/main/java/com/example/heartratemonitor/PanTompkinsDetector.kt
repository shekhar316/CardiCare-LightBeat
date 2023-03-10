package com.example.heartratemonitor

import kotlin.math.*

class PanTompkinsDetector(private val fs: Double) {

    private val LPF = LowpassFilter(5.0, fs)
    private val HPF = HighpassFilter(0.5, fs)
    private val DFF = DifferentiatorFilter(fs)
    private val SQR = SquaringFunction()
    private val MWI = MovingWindowIntegration(0.1, fs)

    fun detect(signal: DoubleArray): List<Int> {
        val filteredSignal = LPF.apply(HPF.apply(signal))
        val diffSignal = DFF.apply(filteredSignal)
        val squaredSignal = SQR.apply(diffSignal)
        val integratedSignal = MWI.apply(squaredSignal)
        val threshold = 0.8 * integratedSignal.max()!!

        val peakIndices = mutableListOf<Int>()
        for (i in 1 until integratedSignal.size - 1) {
            if (integratedSignal[i] > integratedSignal[i - 1] && integratedSignal[i] >= integratedSignal[i + 1]) {
                if (integratedSignal[i] > threshold) {
                    peakIndices.add(i)
                }
            }
        }

        return peakIndices
    }

    private class LowpassFilter(private val fC: Double, private val fs: Double) {

        private val M = 32 // filter order
        private val h = DoubleArray(M + 1)

        init {
            val k = tan(PI * fC / fs)
            for (n in 0..M) {
                h[n] = k.pow(n) / (1 + k.pow(n))
            }
        }

        fun apply(signal: DoubleArray): DoubleArray {
            val y = DoubleArray(signal.size)
            for (n in M until signal.size) {
                y[n] = h[0] * signal[n]
                for (m in 1..M) {
                    y[n] += h[m] * signal[n - m]
                }
            }
            return y
        }
    }

    private class HighpassFilter(private val fC: Double, private val fs: Double) {

        private val M = 32 // filter order
        private val h = DoubleArray(M + 1)

        init {
            val k = tan(PI * fC / fs)
            for (n in 0..M) {
                h[n] = if (n == M / 2) {
                    1.0 - 2.0 * k / (1.0 + k)
                } else {
                    -k.pow(n) / (1 + k.pow(n))
                }
            }
        }

        fun apply(signal: DoubleArray): DoubleArray {
            val y = DoubleArray(signal.size)
            for (n in M until signal.size) {
                y[n] = h[0] * signal[n]
                for (m in 1..M) {
                    y[n] += h[m] * signal[n - m]
                }
            }
            return y
        }
    }

    private class DifferentiatorFilter(private val fs: Double) {

        private val M = 31 // filter order
        private val h = DoubleArray(M + 1)

        init {
            for (n in 0..M) {
                h[n] = when (n - M / 2) {
                    0 -> 0.0
                    else -> -1.0 / (PI * (n - M / 2.0))
                }
            }
        }

//        fun apply(signal: DoubleArray): DoubleArray {
//            val y = DoubleArray(signal.size)
//            for (n in M until signal.size) {
//                y[n] = h[0] * signal[n]
//                for (m in 1..M) {
//                    y[n] += h[m] * (signal[n - m] - signal[n - m - 1])
//                }
//            }
//            return y
//        }
        fun apply(signal: DoubleArray): DoubleArray {
            val y = DoubleArray(signal.size)
            for (n in M + 1 until signal.size) {
                y[n] = h[0] * signal[n]
                for (m in 1..M) {
                    y[n] += h[m] * (signal[n - m] - signal[n - m - 1])
                }
            }
            return y
        }
    }

    private class SquaringFunction {
        fun apply(signal: DoubleArray): DoubleArray {
            return signal.map { x -> x * x }.toDoubleArray()
        }
    }

    private class MovingWindowIntegration(private val windowSize: Double, private val fs: Double) {
        fun apply(signal: DoubleArray): DoubleArray {
            val windowSamples = (windowSize * fs).toInt()
            val y = DoubleArray(signal.size)
            var sum = 0.0
            for (i in signal.indices) {
                sum += signal[i]
                if (i >= windowSamples) {
                    sum -= signal[i - windowSamples]
                }
                y[i] = sum / windowSamples
            }
            return y
        }
    }

}

