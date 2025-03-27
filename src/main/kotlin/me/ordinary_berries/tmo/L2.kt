package me.ordinary_berries.tmo

import me.ordinary_berries.tmo.build.system
import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.impl.MetricStorageImpl
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import me.ordinary_berries.tmo.schema.events.impl.PoissonEventSupplier
import me.ordinary_berries.tmo.schema.events.impl.SimpleEventConstructor
import me.ordinary_berries.tmo.schema.tick.impl.TickerImpl
import me.ordinary_berries.tmo.util.math.ALL_CHANNELS_ARE_FREE_METRIC
import me.ordinary_berries.tmo.util.math.ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.DONE_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.EVENT_PERSIST_METRIC
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY_D
import me.ordinary_berries.tmo.util.math.MINUTES_IN_HOUR
import me.ordinary_berries.tmo.util.SystemRunner
import me.ordinary_berries.tmo.util.math.TIME_IN_QUEUE_METRIC
import me.ordinary_berries.tmo.util.math.allChannelsFreeProbability
import me.ordinary_berries.tmo.util.math.eventWillWaitInAQueueProbability
import me.ordinary_berries.tmo.util.plot.dayTimeRangeMinutes
import me.ordinary_berries.tmo.util.plot.docsDir
import me.ordinary_berries.tmo.util.plot.getDynamic
import me.ordinary_berries.tmo.util.plot.getGroupedByGroupName
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line

private fun createSystemL2(lambda: Double, mu: Double, n: Int): Triple<EventConstructor, EventSupplier, BuiltSystem> {
    val evenPossibility = lambda / MINUTES_IN_HOUR
    val ticksToConsume = MINUTES_IN_HOUR / mu

    val ticker = TickerImpl()
    val metricStorage = MetricStorageImpl(ticker)
    val simpleEventConstructor = SimpleEventConstructor(metricStorage, ticker, ticksToConsume)
    val eventSupplier = PoissonEventSupplier(evenPossibility)

    val system = system(ticker, metricStorage) {
        headNode = nodeBuilder.groupNode(
            subNodes = (1..n).map {
                nodeBuilder.workerNode {
                    queuePersistenceStrategy = queueBuilder.persistAllQueueStrategy()
                }
            }
        ) {
            queuePersistenceStrategy = queueBuilder.persistAllQueueStrategy()
        }
    }

    return Triple(simpleEventConstructor, eventSupplier, system)
}

private fun plotMetrics(filePrefix: String, metrics: Map<String, Counter>) {
    var fileSuffixCounter = 1

    metrics.map { (metric, counter) ->
        counter.getGroupedByGroupName(MINUTES_IN_A_DAY).map { (groupName, dynamic) ->
            plot {
                layout {
                    title = "Metric: $metric; Group: $groupName"
                    xAxisLabel = "day minute"
                    yAxisLabel = "amount"
                }

                line {
                    x(dayTimeRangeMinutes())
                    y(dynamic)
                }
            }.save("$filePrefix${fileSuffixCounter++}.png", path = docsDir(), scale = 1.5, dpi = 300)
        }
    }
}

private fun fixedVarsCase() {
    val lambda = 10.0
    val mu = 5.0
    val n = 4

    val (constructor, supplier, system) = createSystemL2(lambda, mu, n)

    SystemRunner.run(supplier, constructor, MINUTES_IN_HOUR * 24, system)
    plotMetrics("fixed_vars;l:$lambda,m:$mu,n:$n", system.metricStorage.getAllMetrics())
}

private fun waitTimeToNCase() {
    val lambda = 10.0
    val mu = 4.0
    val nRange = (1..10)
    val timeWaitAvg = mutableListOf<Double>()
    val queueSizeAvg = mutableListOf<Double>()

    nRange.map { n ->
        val (constructor, supplier, system) = createSystemL2(lambda, mu, n)
        SystemRunner.run(supplier, constructor, MINUTES_IN_HOUR * 24, system)

        val metrics = system.metricStorage.getAllMetrics()
        timeWaitAvg.add(
            metrics.getDynamic(TIME_IN_QUEUE_METRIC, "SimpleEvent").average()
        )
        queueSizeAvg.add(
            metrics.getDynamic(EVENT_PERSIST_METRIC, "QueuePersistence").average()
        )
    }

    plot {
        layout {
            title = "Metric: $TIME_IN_QUEUE_METRIC"
            xAxisLabel = "n, pts"
            yAxisLabel = "time (min)"
        }

        line {
            x(nRange)
            y(timeWaitAvg)
        }
    }.save("wait_time.png", path = docsDir(), scale = 1.5, dpi = 300)

    plot {
        layout {
            title = "Metric: $EVENT_PERSIST_METRIC"
            xAxisLabel = "n, pts"
            yAxisLabel = "queue size (pts)"
        }

        line {
            x(nRange)
            y(queueSizeAvg)
        }
    }.save("queue_size.png", path = docsDir(), scale = 1.5, dpi = 300)
}

private fun makeTableData() {
    val lambdaRange = (10..20)
    val muRange = (4..10)
    val nRange = (3..10)

    val l2Table = L2Table()

    lambdaRange.map { lambda ->
        val mu = muRange.last().toDouble()
        val n = nRange.first()
        val (constructor, supplier, system) = createSystemL2(
            lambda.toDouble(),
            mu,
            n,
        )
        SystemRunner.run(supplier, constructor, MINUTES_IN_HOUR * 24, system)

        val metrics = system.metricStorage.getAllMetrics()

        l2Table.append(
            lambda.toInt(),
            mu.toInt(),
            n.toInt(),
            metrics,
        )
    }

    muRange.map { mu ->
        val lambda = lambdaRange.first()
        val n = nRange.first()
        val (constructor, supplier, system) = createSystemL2(
            lambda.toDouble(),
            mu.toDouble(),
            n,
        )
        SystemRunner.run(supplier, constructor, MINUTES_IN_HOUR * 24, system)

        val metrics = system.metricStorage.getAllMetrics()

        l2Table.append(
            lambda.toInt(),
            mu.toInt(),
            n.toInt(),
            metrics,
        )
    }

    nRange.map { n ->
        val lambda = lambdaRange.first()
        val mu = muRange.first().toDouble()
        val (constructor, supplier, system) = createSystemL2(
            lambda.toDouble(),
            mu,
            n,
        )
        SystemRunner.run(supplier, constructor, MINUTES_IN_HOUR * 24, system)

        val metrics = system.metricStorage.getAllMetrics()

        l2Table.append(
            lambda.toInt(),
            mu.toInt(),
            n.toInt(),
            metrics,
        )
    }

    l2Table.intoDataframe().writeCSV(docsDir() + "df.csv")
}

private data class L2Table(
    val lambda: MutableList<Int> = mutableListOf(),
    val mu: MutableList<Int> = mutableListOf(),
    val n: MutableList<Int> = mutableListOf(),
    val timeInQueue: MutableList<Double> = mutableListOf(),
    val queueSize: MutableList<Double> = mutableListOf(),
    val doneEvents: MutableList<Int> = mutableListOf(),
    val eventInSystemTime: MutableList<Double> = mutableListOf(),
    val loadCf: MutableList<Double> = mutableListOf(),
    val systemDowntimeProb: MutableList<Double> = mutableListOf(),
    val eventInQueueProb: MutableList<Double> = mutableListOf(),
    val theoreticalProbabilityOfAllChannelsFree: MutableList<Double> = mutableListOf(),
    val theoreticalProbabilityOfEventWaitsInQueue: MutableList<Double> = mutableListOf(),
) {
    fun append(
        lambda: Int,
        mu: Int,
        n: Int,
        metrics: Map<String, Counter>,
    ) {
        val avgTimeInQueue = metrics.getDynamic(TIME_IN_QUEUE_METRIC, "SimpleEvent").average()
        val doneAmount = metrics.getDynamic(DONE_EVENT_METRIC, "WorkerNode").sum()
        val avgQueueSize = metrics.getDynamic(EVENT_PERSIST_METRIC, "QueuePersistence").average()
        val downtimeProb = metrics.getDynamic(ALL_CHANNELS_ARE_FREE_METRIC, "GroupNode").count { it != 0 } / MINUTES_IN_A_DAY_D
        val eventWaitsInQueueProbability = metrics
            .getDynamic(ALL_CHANNELS_ARE_LOCKED_AND_HAVE_NEW_EVENT_METRIC, "GroupNode")
            .count { it != 0 } / MINUTES_IN_A_DAY_D
        val loadCf = (MINUTES_IN_HOUR / mu.toDouble()) * (doneAmount / n.toDouble()) / (MINUTES_IN_HOUR * 24.0)

        this.lambda.add(lambda)
        this.mu.add(mu)
        this.n.add(n)

        this.timeInQueue.add(avgTimeInQueue)
        this.queueSize.add(avgQueueSize)
        this.doneEvents.add(doneAmount)
        this.eventInSystemTime.add(avgTimeInQueue + mu)
        this.loadCf.add(loadCf)
        this.systemDowntimeProb.add(downtimeProb)
        this.eventInQueueProb.add(eventWaitsInQueueProbability)
        this.theoreticalProbabilityOfAllChannelsFree.add(allChannelsFreeProbability(lambda, mu, n))
        this.theoreticalProbabilityOfEventWaitsInQueue.add(eventWillWaitInAQueueProbability(lambda, mu, n))
    }

    fun intoDataframe(): DataFrame<*> {
        return dataFrameOf(
            "lambda" to lambda,
            "mu" to mu,
            "n" to n,
            "timeInQueue" to timeInQueue,
            "queueSize" to queueSize,
            "doneEvents" to doneEvents,
            "eventInSystemTime" to eventInSystemTime,
            "loadCf" to loadCf,
            "systemDowntimeProb" to systemDowntimeProb,
            "eventInQueueProb" to eventInQueueProb,
            "theoreticalProbabilityOfAllChannelsFree" to theoreticalProbabilityOfAllChannelsFree,
            "theoreticalProbabilityOfEventWaitsInQueue" to theoreticalProbabilityOfEventWaitsInQueue,
        )
    }
}

fun runL2() {
    fixedVarsCase()
    waitTimeToNCase()
    makeTableData()
}