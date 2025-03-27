package me.ordinary_berries.tmo.util.math

import kotlin.math.pow

fun alpha(lambda: Int, mu: Int): Double {
    val eventsInMinute = lambda.toDouble() / MINUTES_IN_HOUR_D
    val timeToConsume = MINUTES_IN_HOUR_D / mu.toDouble()
    return eventsInMinute / (1 / timeToConsume)
}

fun allChannelsFreeProbability(lambda: Int, mu: Int, n: Int): Double {
    val alpha = alpha(lambda, mu)

    val a = (0..(n - 1)).sumOf { k -> alpha.pow(k) / k.factorial() }
    val b = alpha.pow(n) / (n.factorial() * (1 - alpha / n.toDouble()))

    return 1.0 / (a + b)
}

fun eventWillWaitInAQueueProbability(lambda: Int, mu: Int, n: Int): Double {
    val alpha = alpha(lambda, mu)

    if (alpha >= n) {
        return 1.0
    }

    return (alpha.pow(n) / n.factorial()) * allChannelsFreeProbability(lambda, mu, n)
}