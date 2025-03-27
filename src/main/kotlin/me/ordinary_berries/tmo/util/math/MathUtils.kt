package me.ordinary_berries.tmo.util.math

fun Double.factorial(): Int = toInt().factorial()

fun Int.factorial(): Int {
    if (this <= 1) {
        return 1
    }

    return (2..this).reduce { a, b -> a * b }
}