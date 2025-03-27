package me.ordinary_berries.tmo.metric.impl

import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.tick.TickSupplier

class MetricStorageImpl(
    private val tickSupplier: TickSupplier,
) : MetricStorage {
    private val counters: MutableMap<String, Counter> = mutableMapOf()

    override fun getCounter(name: String): Counter {
        return counters.getOrPut(name) {
            CounterImpl(name, tickSupplier)
        }
    }

    override fun getAllMetrics(): Map<String, Counter> {
        return counters
    }
}