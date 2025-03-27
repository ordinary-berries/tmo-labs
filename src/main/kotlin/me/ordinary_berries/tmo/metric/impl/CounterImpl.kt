package me.ordinary_berries.tmo.metric.impl

import me.ordinary_berries.tmo.common.Identifiable
import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.MetricMeta
import me.ordinary_berries.tmo.schema.tick.TickSupplier

class CounterImpl(
    private val name: String,
    private val tickSupplier: TickSupplier,
) : Counter {
    private val dynamic: MutableMap<MetricMeta, MutableMap<Int, Int>> = mutableMapOf()

    override fun getName(): String = name

    override fun increment(supplier: Identifiable) = incrementBy(supplier, 1)

    override fun incrementBy(supplier: Identifiable, by: Int) {
        dynamic.getOrPut(getMetricMeta(supplier)) { mutableMapOf() }
            .merge(tickSupplier.getTicked(), by) { a, b -> a + b }
    }

    override fun getDynamic(): Map<MetricMeta, Map<Int, Int>> {
        return dynamic
    }

    private fun getMetricMeta(identifiable: Identifiable): MetricMeta {
        return MetricMeta(
            metricName = name,
            metricSupplierGroup = identifiable.getGroupName(),
            metricSupplierName = identifiable.getName(),
        )
    }
}