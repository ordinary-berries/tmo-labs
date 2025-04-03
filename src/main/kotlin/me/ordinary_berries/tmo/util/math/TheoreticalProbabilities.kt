package me.ordinary_berries.tmo.util.math

import kotlin.math.pow

fun alpha(lambda: Int, mu: Int): Double {
    val eventsInMinute = lambda / MINUTES_IN_HOUR_D
    val timeToConsume = MINUTES_IN_HOUR_D / mu
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

fun allChannelsFreeProbabilityWithLimit(lambda: Int, mu: Int, n: Int, m: Int): Double {
    val alpha = lambda.toDouble() / mu.toDouble()

    return 1.0 /
        ((0..n).sumOf { a -> alpha.pow(a) / a.factorial() } +
            (1..m).sumOf { a -> alpha.pow(n + a) / (n.factorial() * n.toDouble().pow(a)) })
}

fun dropProbabilityWithLimit(lambda: Int, mu: Int, n: Int, m: Int): Double {
    val alpha = lambda.toDouble() / mu.toDouble()

    return (alpha.pow(n + m) / (n.toDouble().pow(m) * n.factorial())) * allChannelsFreeProbabilityWithLimit(
        lambda,
        mu,
        n,
        m
    )
}

fun averageAmountOfEventsInQueueWithLimit(lambda: Int, mu: Int, n: Int, m: Int): Double {
    val alpha = lambda.toDouble() / mu.toDouble()

    return if (alpha != n.toDouble()) {
        (alpha.pow(n + 1) / (n * n.factorial())) *
            ((1 - (alpha / n).pow(m) * (m + 1 - ((m * alpha) / n))) / (1 - (alpha / n)).pow(2)) *
            allChannelsFreeProbabilityWithLimit(lambda, mu, n, m)
    } else {
        (alpha.pow(n + 1) / (n * n.factorial())) *
            (0..(m - 1)).sumOf { a -> (a + 1) * (alpha / n).pow(a) } *
            allChannelsFreeProbabilityWithLimit(lambda, mu, n, m)
    }
}
