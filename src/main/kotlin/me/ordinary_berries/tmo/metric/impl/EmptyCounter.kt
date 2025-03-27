package me.ordinary_berries.tmo.metric.impl

import me.ordinary_berries.tmo.common.Identifiable
import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.MetricMeta

class EmptyCounter(
    private val name: String,
) : Counter {
    override fun increment(supplier: Identifiable) {}

    override fun incrementBy(supplier: Identifiable, by: Int) {}

    override fun getName(): String = name

    override fun getDynamic(): Map<MetricMeta, Map<Int, Int>> = mapOf()
}