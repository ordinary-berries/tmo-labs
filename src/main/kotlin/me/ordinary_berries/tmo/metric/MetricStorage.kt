package me.ordinary_berries.tmo.metric

interface MetricStorage {
    fun getCounter(name: String): Counter

    fun getAllMetrics(): Map<String, Counter>
}