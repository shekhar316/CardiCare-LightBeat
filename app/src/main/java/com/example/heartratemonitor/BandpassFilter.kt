package com.example.heartratemonitor

import kotlin.math.*

class BandpassFilter(private val fL: Double, private val fH: Double, private val fs: Double) {

    private val M = 51 // filter order
    private val h = DoubleArray(M + 1)

    init {
        val fC = (fL + fH) / 2.0
        val k = tan(PI * fC / fs)
        val Q = fC / (fH - fL)
        val a0 = k / (1 + Q * k + k * k)
        val a1 = 2 * a0
        val a2 = a0
        val b1 = 2 * a0 * (k * k - 1) / (1 + Q * k + k * k)
        val b2 = a0 * (1 - Q * k + k * k) / (1 + Q * k + k * k)

        for (n in 0..M) {
            h[n] = when (n) {
                0 -> a0
                1 -> a1
                2 -> a2
                else -> b1 * h[n - 1] - b2 * h[n - 2]
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

//class BandpassFilter(private val fL: Double, private val fH: Double, private val b: Double) {
//
//    fun apply(signal: DoubleArray, fs: Double): DoubleArray {
//        val dt = 1.0 / fs
//        val t = DoubleArray(signal.size) { it * dt }
//        val filter = DoubleArray(signal.size) { i ->
//            if (i == 0) 0.0 else {
//                val alpha = 2 * Math.PI * b * dt
//                val beta = (fH + fL) * 2 * Math.PI * dt / 2.0
//                val gamma = 2 * Math.PI * fL * fH * dt * dt
//                val a1 = 2 + alpha
//                val a2 = 1 - alpha + beta
//                val a3 = 2 - beta - gamma
//                val b1 = alpha
//                val b2 = -1 + alpha - beta
//                val b3 = beta - gamma
//                (b1 * signal[i - 1] + b2 * signal[i - 2] + b3 * signal[i - 3] -
//                        a2 * filter[i - 1] - a3 * filter[i - 2]) / a1
//            }
//        }
//        return filter
//    }
//}


