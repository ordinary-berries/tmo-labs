package me.ordinary_berries.tmo

import me.ordinary_berries.tmo.build.system
import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.impl.MetricStorageImpl
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import me.ordinary_berries.tmo.schema.events.impl.PoissonEventSupplier
import me.ordinary_berries.tmo.schema.events.impl.SimpleEventConstructor
import me.ordinary_berries.tmo.schema.tick.impl.TickerImpl
import me.ordinary_berries.tmo.util.SystemRunner
import me.ordinary_berries.tmo.util.math.DONE_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.DROP_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.EVENT_PERSIST_METRIC
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY_D
import me.ordinary_berries.tmo.util.math.MINUTES_IN_HOUR_D
import me.ordinary_berries.tmo.util.math.TIME_IN_SYSTEM_METRIC
import me.ordinary_berries.tmo.util.math.averageAmountOfEventsInQueueWithLimit
import me.ordinary_berries.tmo.util.math.dropProbabilityWithLimit
import me.ordinary_berries.tmo.util.plot.dayTimeRangeMinutes
import me.ordinary_berries.tmo.util.plot.docsDir
import me.ordinary_berries.tmo.util.plot.getDynamic
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.hLine
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.letsplot.settings.LineType
import org.jetbrains.kotlinx.kandy.util.color.Color

private fun createSystemL3(lambda: Int, mu: Int, m: Int): Triple<EventConstructor, EventSupplier, BuiltSystem> {
    val evenPossibility = lambda / MINUTES_IN_HOUR_D
    val ticksToConsume = MINUTES_IN_HOUR_D / mu

    val ticker = TickerImpl()
    val metricStorage = MetricStorageImpl(ticker)
    val simpleEventConstructor = SimpleEventConstructor(metricStorage, ticker, ticksToConsume)
    val eventSupplier = PoissonEventSupplier(evenPossibility)

    val system = system(ticker, metricStorage) {
        headNode = nodeBuilder.workerNode {
            queuePersistenceStrategy = queueBuilder.dropQueueOnFixedSizeStrategy { maximumQueueSize = m }
        }
    }

    return Triple(simpleEventConstructor, eventSupplier, system)
}

private fun createSystemRunAndGetMetrics(lambda: Int, mu: Int, m: Int): Map<String, Counter> {
    val (constructor, supplier, builtSystem) = createSystemL3(lambda, mu, m)
    SystemRunner.run(supplier, constructor, MINUTES_IN_A_DAY, builtSystem)

    return builtSystem.metricStorage.getAllMetrics()
}

private fun fixedVarsCase() {
    val lambda = 8
    val mu = 10
    val m = 5

    val metrics = createSystemRunAndGetMetrics(lambda, mu, m)

    plot {
        line {
            layout {
                title = "Metric: $EVENT_PERSIST_METRIC"
                xAxisLabel = "minute"
                yAxisLabel = "amount of events persisted (pts)"
            }

            x(dayTimeRangeMinutes())
            y(metrics.getDynamic(EVENT_PERSIST_METRIC, "QueuePersistence"))
        }
    }.save("fixedVarsCase.l:$lambda;mu:$mu;m:$m;persisted.png", scale = 1.5, dpi = 300, path = docsDir())

    plot {
        line {
            layout {
                title = "Metric: $DROP_EVENT_METRIC"
                xAxisLabel = "minute"
                yAxisLabel = "amount of events dropped (pts)"
            }

            x(dayTimeRangeMinutes())
            y(metrics.getDynamic(DROP_EVENT_METRIC, "QueuePersistence"))
        }
    }.save("fixedVarsCase.l:$lambda;mu:$mu;m:$m;dropped.png", scale = 1.5, dpi = 300, path = docsDir())

    plot {
        line {
            layout {
                title = "Metric: $TIME_IN_SYSTEM_METRIC"
                xAxisLabel = "minute"
                yAxisLabel = "time spent in queue (min)"
            }

            x(dayTimeRangeMinutes())
            y(metrics.getDynamic(TIME_IN_SYSTEM_METRIC, "SimpleEvent"))
        }
    }.save("fixedVarsCase.l:$lambda;mu:$mu;m:$m;time_in_queue.png", scale = 1.5, dpi = 300, path = docsDir())
}

private fun tableCase() {
    val lambdas = (8..13)
    val mus = (10..15)
    val ms = (5..10)

    val table = L3Table()

    lambdas.map { lambda ->
        val mu = mus.last()
        val m = ms.last()

        val metrics = createSystemRunAndGetMetrics(lambda, mu, m)
        table.append(lambda, mu, m, metrics)
    }

    mus.map { mu ->
        val lambda = lambdas.last()
        val m = ms.last()

        val metrics = createSystemRunAndGetMetrics(lambda, mu, m)
        table.append(lambda, mu, m, metrics)
    }

    ms.map { m ->
        val lambda = lambdas.first()
        val mu = mus.first()

        val metrics = createSystemRunAndGetMetrics(lambda, mu, m)
        table.append(lambda, mu, m, metrics)
    }

    table.intoDataFrame().writeCSV(docsDir() + "l3_df.csv")
}

private fun Map<String, Counter>.getLoss(): Double =
    getDynamic(DROP_EVENT_METRIC, "QueuePersistence").sum() / getDynamic(DONE_EVENT_METRIC, "WorkerNode").sum()
        .toDouble()

private fun optimizeLossCase() {
    val lambda = 5
    val mu = 6
    val ms = (1..20)
    val runAmount = 20

    val lossDynamic: MutableList<Double> = mutableListOf()
    val theoreticalLossDynamic: MutableList<Double> = mutableListOf()
    val waitTimeDynamic: MutableList<Double> = mutableListOf()

    ms.map { m ->
        var lossTotal = 0.0
        var avgWaitTimeTotal = 0.0

        (1..runAmount).forEach {
            val metrics = createSystemRunAndGetMetrics(lambda, mu, m)
            lossTotal += metrics.getLoss()
            avgWaitTimeTotal += metrics.getDynamic(TIME_IN_SYSTEM_METRIC, "SimpleEvent").average()
        }

        lossDynamic.add(lossTotal / runAmount)
        theoreticalLossDynamic.add(dropProbabilityWithLimit(lambda, mu, 1, m))
        waitTimeDynamic.add(avgWaitTimeTotal / runAmount)
    }

    plot {
        line {
            x(ms)
            y(theoreticalLossDynamic)
            color = Color.BLUE
        }

        line {
            x(ms)
            y(lossDynamic)
            color = Color.RED
        }

        hLine {
            yIntercept.constant(0.05) // Target is 5%
            color = Color.GREEN
            type = LineType.DASHED
        }

        layout {
            title = "Loss optimization: RED - model avg in $runAmount runs, BLUE - theoretical"
            xAxisLabel = "m (queue length, event pts)"
            yAxisLabel = "Loss (probability)"
        }
    }.save("loss_optimization_loss.png", scale = 1.5, dpi = 300, docsDir())

    plot {
        line {
            x(ms)
            y(waitTimeDynamic)
            color = Color.RED
        }

        layout {
            title = "Loss optimization (wait time)"
            xAxisLabel = "m (queue length, event pts)"
            yAxisLabel = "Wait time (minutes)"
        }
    }.save("loss_optimization_wait_time.png", scale = 1.5, dpi = 300, docsDir())
}

private data class L3Table(
    val lambda: MutableList<Int> = mutableListOf(),
    val mu: MutableList<Int> = mutableListOf(),
    val m: MutableList<Int> = mutableListOf(),
    val dropEventProbability: MutableList<Double> = mutableListOf(),
    val theoreticalDropEventProbability: MutableList<Double> = mutableListOf(),
    val amountOfEventsInQueueAvg: MutableList<Double> = mutableListOf(),
    val theoreticalAmountOfEventsInQueueAvg: MutableList<Double> = mutableListOf(),
    val waitTimeInQueueAvg: MutableList<Double> = mutableListOf(),
    val eventInSystemTimeAvg: MutableList<Double> = mutableListOf(),
    val doneEventsTotal: MutableList<Int> = mutableListOf(),
    val loadCf: MutableList<Double> = mutableListOf(),
) {
    fun append(
        lambda: Int,
        mu: Int,
        m: Int,
        metrics: Map<String, Counter>,
    ) {
        this.lambda.add(lambda)
        this.mu.add(mu)
        this.m.add(m)
        val doneAmount = metrics.getDynamic(DONE_EVENT_METRIC, "WorkerNode").sum()

        val dropProb = metrics.getDynamic(DROP_EVENT_METRIC, "QueuePersistence").sum() / doneAmount.toDouble()
        val theoreticalDropProb = dropProbabilityWithLimit(lambda, mu, 1, m)
        val avgEventsInQueue = metrics.getDynamic(EVENT_PERSIST_METRIC, "QueuePersistence").average()
        val theoreticalAvgEventsInQueue = averageAmountOfEventsInQueueWithLimit(lambda, mu, 1, m)
        val avgWaitInQueue = metrics.getDynamic(TIME_IN_SYSTEM_METRIC, "SimpleEvent").average()
        val loadCf = (doneAmount * (MINUTES_IN_HOUR_D / mu)) / MINUTES_IN_A_DAY_D
        val totalDoneEvents = metrics.getDynamic(DONE_EVENT_METRIC, "WorkerNode").sum()

        this.dropEventProbability.add(dropProb)
        this.theoreticalDropEventProbability.add(theoreticalDropProb)
        this.amountOfEventsInQueueAvg.add(avgEventsInQueue)
        this.theoreticalAmountOfEventsInQueueAvg.add(theoreticalAvgEventsInQueue)
        this.waitTimeInQueueAvg.add(avgWaitInQueue)
        this.eventInSystemTimeAvg.add(avgWaitInQueue + (MINUTES_IN_HOUR_D / mu))
        this.doneEventsTotal.add(totalDoneEvents)
        this.loadCf.add(loadCf)
    }

    fun intoDataFrame(): DataFrame<*> {
        return dataFrameOf(
            "lambda" to lambda,
            "mu" to mu,
            "m" to m,
            "dropEventProbability" to dropEventProbability,
            "theoreticalDropEventProbability" to theoreticalDropEventProbability,
            "amountOfEventsInQueueAvg" to amountOfEventsInQueueAvg,
            "theoreticalAvgEventsInQueue" to theoreticalAmountOfEventsInQueueAvg,
            "waitTimeInQueueAvg" to waitTimeInQueueAvg,
            "eventInSystemTimeAvg" to eventInSystemTimeAvg,
            "doneEventsTotal" to doneEventsTotal,
            "loadCf" to loadCf,
        )
    }
}

fun runL3() {
    fixedVarsCase()
    tableCase()
    optimizeLossCase()
}