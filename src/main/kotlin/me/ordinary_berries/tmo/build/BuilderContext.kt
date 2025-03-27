package me.ordinary_berries.tmo.build

import me.ordinary_berries.tmo.metric.MetricStorage
import me.ordinary_berries.tmo.metric.impl.MetricStorageImpl
import me.ordinary_berries.tmo.schema.tick.Ticker

data class BuilderContext(
    val ticker: Ticker,
    private var _metricStorage: MetricStorage? = null,
) {
    val defaultMetricStorage = lazy { MetricStorageImpl(ticker) }
    val metricStorage: MetricStorage = _metricStorage ?: defaultMetricStorage.value
}
