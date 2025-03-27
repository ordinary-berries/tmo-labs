package me.ordinary_berries.tmo

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.tick.Ticker

data class BuiltSystem(
    val headNode: Node,
    val metricStorage: MetricStorage,
    val ticker: Ticker,
)