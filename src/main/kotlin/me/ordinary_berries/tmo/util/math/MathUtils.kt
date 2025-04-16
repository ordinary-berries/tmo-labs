package me.ordinary_berries.tmo.util.math

import kotlin.math.exp
import kotlin.random.Random

fun Double.factorial(): Int = toInt().factorial()

fun Int.factorial(): Int {
    if (this <= 1) {
        return 1
    }

    return (2..this).reduce { a, b -> a * b }
}

fun poissonDistribution(lambda: Double): Int {
    val l = exp(-lambda)
    var k = 0
    var p = 1.0

    while (true) {
        p *= Random.nextDouble()
        if (p > l) {
            k++
        } else {
            return k
        }
    }
}
