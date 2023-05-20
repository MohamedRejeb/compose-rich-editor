package com.mohamedrejeb.richeditor.utils

import kotlin.math.pow
import kotlin.math.roundToInt

fun Float.maxDecimals(decimals: Int): Float {
    val multiplier = 10.0.pow(decimals.toDouble()).toFloat()
    return (this * multiplier).roundToInt() / multiplier
}