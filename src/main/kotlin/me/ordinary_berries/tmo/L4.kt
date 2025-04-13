package me.ordinary_berries.tmo

import me.ordinary_berries.tmo.build.system
import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.impl.MetricStorageImpl
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import me.ordinary_berries.tmo.schema.events.impl.PoissonEventSupplier
import me.ordinary_berries.tmo.schema.events.impl.PrioritizedEventConstructor
import me.ordinary_berries.tmo.schema.tick.impl.TickerImpl
import me.ordinary_berries.tmo.util.SystemRunner
import me.ordinary_berries.tmo.util.math.DONE_EVENT_METRIC
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY_D
import me.ordinary_berries.tmo.util.math.MINUTES_IN_HOUR_D
import me.ordinary_berries.tmo.util.math.alpha
import me.ordinary_berries.tmo.util.math.averageAmountOfQueueWithPriorities
import me.ordinary_berries.tmo.util.math.averageWaitTimeInQueueWithPriorities
import me.ordinary_berries.tmo.util.math.getPrioritizedEventPersistMetricName
import me.ordinary_berries.tmo.util.math.getPrioritizedTimeInSystemMetricName
import me.ordinary_berries.tmo.util.plot.dayTimeRangeMinutes
import me.ordinary_berries.tmo.util.plot.docsDir
import me.ordinary_berries.tmo.util.plot.getDynamic
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.dataframe.math.median
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.save
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import org.jetbrains.kotlinx.kandy.util.color.Color

private const val hiPriority: Int = 1
private const val loPriority: Int = 2

private fun createSystemL4(
    lambdaHi: Int,
    lambdaLo: Int,
    mu: Int,
): Triple<EventConstructor, EventSupplier, BuiltSystem> {
    val evenDensityHi = lambdaHi / MINUTES_IN_HOUR_D
    val evenDensityLo = lambdaLo / MINUTES_IN_HOUR_D
    val ticksToConsume = MINUTES_IN_HOUR_D / mu

    val ticker = TickerImpl()
    val metricStorage = MetricStorageImpl(ticker)
    val prioritizedEventConstructor = PrioritizedEventConstructor(
        metricStorage,
        ticker,
        ticksToConsume,
        hiPriority to evenDensityHi,
        loPriority to evenDensityLo,
    )
    val eventSupplier = PoissonEventSupplier(evenDensityHi + evenDensityLo)

    val system = system(ticker, metricStorage) {
        headNode = nodeBuilder.workerNode {
            queuePersistenceStrategy = queueBuilder.persistAllQueueStrategy()
        }
    }

    return Triple(prioritizedEventConstructor, eventSupplier, system)
}

private fun createSystemRunAndGetMetrics(lambdaHi: Int, lambdaLo: Int, mu: Int): Map<String, Counter> {
    val (constructor, supplier, builtSystem) = createSystemL4(lambdaHi, lambdaLo, mu)
    SystemRunner.run(supplier, constructor, MINUTES_IN_A_DAY, builtSystem)

    return builtSystem.metricStorage.getAllMetrics()
}

private fun fixedVarsCase() {
    val lambdaHi = 3
    val lambdaLo = 5
    val mu = 10

    val metrics = createSystemRunAndGetMetrics(lambdaHi, lambdaLo, mu)
    val hiTimeInQueue = metrics.getDynamic(getPrioritizedTimeInSystemMetricName(hiPriority), "PrioritizedEvent")
        .map { it - (MINUTES_IN_HOUR_D / mu) }
    val loTimeInQueue = metrics.getDynamic(getPrioritizedTimeInSystemMetricName(loPriority), "PrioritizedEvent")
        .map { it - (MINUTES_IN_HOUR_D / mu) }
    val hiTimeInSystem = metrics.getDynamic(getPrioritizedTimeInSystemMetricName(hiPriority), "PrioritizedEvent")
    val loTimeInSystem = metrics.getDynamic(getPrioritizedTimeInSystemMetricName(loPriority), "PrioritizedEvent")

    plot {
        line {
            x(dayTimeRangeMinutes())
            y(hiTimeInSystem)

            layout {
                color = Color.RED
            }
        }

        line {
            x(dayTimeRangeMinutes())
            y(loTimeInSystem)

            layout {
                color = Color.GREEN
            }
        }

        layout {
            title = "Time in system. RED - high priority; GREEN - low priority"
            xAxisLabel = "day minutes"
            yAxisLabel = "time in system (minutes)"
        }
    }.save(
        "fixedVars_timeInSystem_lHi=${lambdaHi}_lLo=${lambdaLo}_mu=${mu}.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )

    plot {
        line {
            x(dayTimeRangeMinutes())
            y(hiTimeInQueue)

            layout {
                color = Color.RED
            }
        }

        line {
            x(dayTimeRangeMinutes())
            y(loTimeInQueue)

            layout {
                color = Color.GREEN
            }
        }

        layout {
            title = "Time in queue. RED - high priority; GREEN - low priority"
            xAxisLabel = "day minutes"
            yAxisLabel = "time in queue (minutes)"
        }
    }.save(
        "fixedVars_timeInQueue_lHi=${lambdaHi}_lLo=${lambdaLo}_mu=${mu}.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )
}

private fun tableCase() {
    val table = L4Table()

    val lambdaHis = (2..6)
    val lambdaLos = (4..8)
    val mus = (9..12)

    lambdaHis.map { lambdaHi ->
        val lambdaLo = lambdaLos.first
        val mu = mus.last
        val metrics = createSystemRunAndGetMetrics(lambdaHi, lambdaLo, mu)

        table.append(lambdaHi, lambdaLo, mu, metrics)
    }

    lambdaLos.map { lambdaLo ->
        val lambdaHi = lambdaHis.first
        val mu = mus.last
        val metrics = createSystemRunAndGetMetrics(lambdaHi, lambdaLo, mu)

        table.append(lambdaHi, lambdaLo, mu, metrics)
    }

    mus.map { mu ->
        val lambdaHi = lambdaHis.first
        val lambdaLo = lambdaLos.first
        val metrics = createSystemRunAndGetMetrics(lambdaHi, lambdaLo, mu)

        table.append(lambdaHi, lambdaLo, mu, metrics)
    }

    table.intoDataFrame().writeCSV(docsDir() + "lab4.csv")
}

private fun plotCase() {
    val lambdaLos = (1..20)
    val lambdaHis = (1..20)
    val mu = 32
    val muTime = mu / MINUTES_IN_HOUR_D

    val lambdaHi = lambdaHis.median()
    val lambdaLo = lambdaLos.median()

    val losAvgWaitTime = mutableListOf<Double>()
    val hisAvgWaitTime = mutableListOf<Double>()

    lambdaLos.map { lambdaLo ->
        val metrics = createSystemRunAndGetMetrics(lambdaHi, lambdaLo, mu)
        val avgWaitTime =
            metrics.getDynamic(getPrioritizedTimeInSystemMetricName(hiPriority), "PrioritizedEvent")
                .map { if (it > 0) it.toDouble() - muTime else it.toDouble()}.average()

        losAvgWaitTime.add(avgWaitTime)
    }

    lambdaHis.map { lambdaHi ->
        val metrics = createSystemRunAndGetMetrics(lambdaHi, lambdaLo, mu)
        val avgWaitTime =
            metrics.getDynamic(getPrioritizedTimeInSystemMetricName(hiPriority), "PrioritizedEvent")
                .map { if (it > 0) it.toDouble() - muTime else it.toDouble()}.average()

        hisAvgWaitTime.add(avgWaitTime)
    }

    plot {
        line {
            x(lambdaHis)
            y(hisAvgWaitTime)

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "High priority events wait time, l_lo=$lambdaLo"
            xAxisLabel = "lambdas"
            yAxisLabel = "time in queue (minutes)"
        }
    }.save("wait_time_hi.png", scale = 1.5, dpi = 300, path = docsDir())

    plot {
        line {
            x(lambdaLos)
            y(losAvgWaitTime)

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "Low priority events wait time, l_hi=$lambdaHi"
            xAxisLabel = "lambdas"
            yAxisLabel = "time in queue (minutes)"
        }
    }.save("wait_time_lo.png", scale = 1.5, dpi = 300, path = docsDir())
}

private data class L4Table(
    val lambdaHi: MutableList<Int> = mutableListOf(),
    val lambdaLo: MutableList<Int> = mutableListOf(),
    val mu: MutableList<Int> = mutableListOf(),
    val loadCf: MutableList<Double> = mutableListOf(),
    val averageAmountOfEventsInSystem: MutableList<Double> = mutableListOf(),
    val theoreticalAverageAmountOfEventsInSystem: MutableList<Double> = mutableListOf(),
    val averageTimeInSystemHi: MutableList<Double> = mutableListOf(),
    val averageTimeInSystemLo: MutableList<Double> = mutableListOf(),
    val theoreticalTimeInSystemHi: MutableList<Double> = mutableListOf(),
    val theoreticalTimeInSystemLo: MutableList<Double> = mutableListOf(),
    val averageTimeInQueueHi: MutableList<Double> = mutableListOf(),
    val averageTimeInQueueLo: MutableList<Double> = mutableListOf(),
    val theoreticalTimeInQueueHi: MutableList<Double> = mutableListOf(),
    val theoreticalTimeInQueueLo: MutableList<Double> = mutableListOf(),
    val probabilityOfWaitingInQueue: MutableList<Double> = mutableListOf(),
) {
    fun append(
        lambdaHi: Int,
        lambdaLo: Int,
        mu: Int,
        metrics: Map<String, Counter>
    ) {
        this.lambdaHi.add(lambdaHi)
        this.lambdaLo.add(lambdaLo)
        this.mu.add(mu)

        val muTime = mu / MINUTES_IN_HOUR_D
        val doneAmount = metrics.getDynamic(DONE_EVENT_METRIC, "WorkerNode").sum()
        val loadCf = (doneAmount * (MINUTES_IN_HOUR_D / mu)) / MINUTES_IN_A_DAY_D
        val averageAmountOfEvents =
            (metrics.getDynamic(
                getPrioritizedEventPersistMetricName(hiPriority),
                "QueuePersistence"
            ) + metrics.getDynamic(
                getPrioritizedEventPersistMetricName(loPriority),
                "QueuePersistence"
            )).average()
        val averageTimeInSystemHi =
            metrics.getDynamic(getPrioritizedTimeInSystemMetricName(hiPriority), "PrioritizedEvent")
                .map { if (it > 0) it.toDouble() - muTime else it.toDouble() }
                .average()
        val averageTimeInSystemLo =
            metrics.getDynamic(getPrioritizedTimeInSystemMetricName(loPriority), "PrioritizedEvent")
                .map { if (it > 0) it.toDouble() - muTime else it.toDouble() }
                .average()
        val theoreticalTimeInQueueHi =
            averageWaitTimeInQueueWithPriorities(hiPriority, listOf(lambdaHi, lambdaLo), mu)
        val theoreticalTimeInQueueLo =
            averageWaitTimeInQueueWithPriorities(loPriority, listOf(lambdaHi, lambdaLo), mu)

        val amountOfEventsInQueueLo = averageAmountOfQueueWithPriorities(
            loPriority,
            listOf(lambdaHi, lambdaLo),
            mu,
        )

        val amountOfEventsInQueueHi = averageAmountOfQueueWithPriorities(
            hiPriority,
            listOf(lambdaHi, lambdaLo),
            mu,
        )

        this.loadCf.add(loadCf)
        this.averageAmountOfEventsInSystem.add(averageAmountOfEvents)
        this.theoreticalAverageAmountOfEventsInSystem.add((amountOfEventsInQueueHi + amountOfEventsInQueueLo) / 2.0)
        this.averageTimeInSystemHi.add(averageTimeInSystemHi)
        this.averageTimeInSystemLo.add(averageTimeInSystemLo)
        this.theoreticalTimeInSystemHi.add(theoreticalTimeInQueueHi + muTime)
        this.theoreticalTimeInSystemLo.add(theoreticalTimeInQueueLo + muTime)
        this.averageTimeInQueueHi.add(averageTimeInSystemHi - muTime)
        this.averageTimeInQueueLo.add(averageTimeInSystemLo - muTime)
        this.theoreticalTimeInQueueHi.add(theoreticalTimeInQueueHi)
        this.theoreticalTimeInQueueLo.add(theoreticalTimeInQueueLo)
        this.probabilityOfWaitingInQueue.add(alpha(lambdaHi + lambdaLo, mu))
    }

    fun intoDataFrame(): DataFrame<*> {
        return dataFrameOf(
            "lambdaHi" to lambdaHi,
            "lambdaLo" to lambdaLo,
            "mu" to mu,
            "loadCf" to loadCf,
            "averageAmountOfEventsInSystem" to averageAmountOfEventsInSystem,
            "theoreticalAverageAmountOfEventsInSystem" to theoreticalAverageAmountOfEventsInSystem,
            "averageTimeInSystemLo" to averageTimeInSystemLo,
            "averageTimeInSystemHi" to averageTimeInSystemHi,
            "theoreticalTimeInSystemLo" to theoreticalTimeInSystemLo,
            "theoreticalTimeInSystemHi" to theoreticalTimeInSystemHi,
            "averageTimeInQueueLo" to averageTimeInQueueLo,
            "averageTimeInQueueHi" to averageTimeInQueueHi,
            "theoreticalTimeInQueueLo" to theoreticalTimeInQueueLo,
            "theoreticalTimeInQueueHi" to theoreticalTimeInQueueHi,
            "probabilityOfWaitingInQueue" to probabilityOfWaitingInQueue,
        )
    }
}

fun runL4() {
    fixedVarsCase()
    plotCase()
    tableCase()
}