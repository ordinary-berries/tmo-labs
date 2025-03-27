package me.ordinary_berries.tmo.metric

import me.ordinary_berries.tmo.common.Identifiable

interface Counter {
    fun increment(supplier: Identifiable)
    fun incrementBy(supplier: Identifiable, by: Int)

    fun getName(): String

    fun getDynamic(): Map<MetricMeta, Map<Int, Int>>
}