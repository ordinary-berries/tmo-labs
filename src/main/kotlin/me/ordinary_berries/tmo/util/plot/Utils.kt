package me.ordinary_berries.tmo.util.plot

import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.MetricMeta
import me.ordinary_berries.tmo.metric.impl.EmptyCounter
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY
import javax.swing.JFileChooser

fun dayTimeRangeMinutes(): List<Int> =
    (1..MINUTES_IN_A_DAY).toList()

fun Map<String, Counter>.getDynamic(
    metricName: String,
    groupName: String,
    toSize: Int = MINUTES_IN_A_DAY,
): List<Int> {
    return getCounter(metricName).getGroupedByGroupName(toSize).getOrElse(groupName) { listOf() }
}

fun Map<String, Counter>.getGrouped(groupName: String): Map<String, List<Int>> { // Metric to counters
    return mapNotNull { (metricName, counter) ->
        counter.getGroupedByGroupName(MINUTES_IN_A_DAY)[groupName]?.let {
            metricName to it
        }
    }.toMap()
}

fun Map<String, Counter>.getCounter(metricName: String): Counter =
    getOrElse(metricName) { EmptyCounter(metricName) }

fun Counter.getGroupedByGroupName(lengthOfPeriod: Int): Map<String, List<Int>> =
    getDynamic().mergeByTransformer { it.metricSupplierGroup }.mapValues { it.value.fillGapsUpTo(lengthOfPeriod) }

fun docsDir(): String =
    JFileChooser().fileSystemView.defaultDirectory.toString() + "/tmo_plots/"

private fun Map<MetricMeta, Map<Int, Int>>.mergeByTransformer(transformer: (MetricMeta) -> String): Map<String, Map<Int, Int>> {
    val result = mutableMapOf<String, MutableMap<Int, Int>>()

    forEach { (meta, metrics) ->
        result.compute(transformer(meta)) { transformed, maybeExisting ->
            if (maybeExisting == null) {
                metrics.toMutableMap()
            } else {
                metrics.map { (tick, inc) ->
                    maybeExisting.merge(tick, inc) { a, b -> a + b }
                }
                maybeExisting
            }
        }
    }

    return result
}

private fun Map<Int, Int>.fillGapsUpTo(n: Int): List<Int> {
    val result = MutableList(n) { 0 }

    (0..(n - 1)).forEach {
        result[it] += get(it) ?: 0
    }

    return result
}

