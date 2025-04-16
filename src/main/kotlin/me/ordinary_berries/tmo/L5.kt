package me.ordinary_berries.tmo

import me.ordinary_berries.tmo.build.system
import me.ordinary_berries.tmo.metric.Counter
import me.ordinary_berries.tmo.metric.impl.MetricStorageImpl
import me.ordinary_berries.tmo.schema.events.EventConstructor
import me.ordinary_berries.tmo.schema.events.EventSupplier
import me.ordinary_berries.tmo.schema.events.impl.PoissonEventSupplier
import me.ordinary_berries.tmo.schema.events.impl.SimpleEventConstructor
import me.ordinary_berries.tmo.schema.nodes.impl.PoissonNodeSupplier
import me.ordinary_berries.tmo.schema.nodes.impl.WorkerNodeConstructor
import me.ordinary_berries.tmo.schema.tick.impl.TickerImpl
import me.ordinary_berries.tmo.util.SystemRunner
import me.ordinary_berries.tmo.util.math.DYNAMIC_GROUP_AGENTS_AMOUNT_METRIC
import me.ordinary_berries.tmo.util.math.MINUTES_IN_A_DAY
import me.ordinary_berries.tmo.util.math.MINUTES_IN_HOUR_D
import me.ordinary_berries.tmo.util.math.TIME_IN_SYSTEM_METRIC
import me.ordinary_berries.tmo.util.math.getPrioritizedEventPersistMetricName
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

private fun createSystemL5(
    lambda: Int,
    agentAddL: Double,
    agentRemoveL: Double,
    minAgentAmount: Int,
    maxAgentAmount: Int,
    mu: Int,
): Triple<EventConstructor, EventSupplier, BuiltSystem> {
    require(minAgentAmount <= maxAgentAmount)

    val eventDensity = lambda / MINUTES_IN_HOUR_D
    val agentAddDensity = agentAddL / MINUTES_IN_HOUR_D
    val agentRemoveDensity = agentRemoveL / MINUTES_IN_HOUR_D
    val ticksToConsume = MINUTES_IN_HOUR_D / mu

    val ticker = TickerImpl()
    val metricStorage = MetricStorageImpl(ticker)
    val nodeSupplier = PoissonNodeSupplier(agentAddDensity)
    val eventConstructor = SimpleEventConstructor(metricStorage, ticker, ticksToConsume)
    val eventSupplier = PoissonEventSupplier(eventDensity)

    val system = system(ticker, metricStorage) {
        val nodeConstructor = WorkerNodeConstructor(
            queueBuilder.persistAllQueueStrategy(),
            builderContext.metricStorage,
            builderContext.ticker
        )

        headNode = nodeBuilder.dynamicGroupNode(nodeConstructor, nodeSupplier) {
            minSize = minAgentAmount
            maxSize = maxAgentAmount
            shrinkDensity = agentRemoveDensity
            queuePersistenceStrategy = queueBuilder.persistAllQueueStrategy()
        }
    }

    return Triple(eventConstructor, eventSupplier, system)
}

private fun createSystemRunAndGetMetrics(
    lambda: Int,
    agentAddDensity: Double,
    agentRemoveDensity: Double,
    minAgentAmount: Int,
    maxAgentAmount: Int,
    mu: Int,
): Map<String, Counter> {
    val (constructor, supplier, builtSystem) = createSystemL5(
        lambda,
        agentAddDensity,
        agentRemoveDensity,
        minAgentAmount,
        maxAgentAmount,
        mu,
    )
    SystemRunner.run(supplier, constructor, MINUTES_IN_A_DAY, builtSystem)

    return builtSystem.metricStorage.getAllMetrics()
}

private fun fixedVarsCase() {
    val lambda = 10
    val mu = 5
    val agentsMin = 1
    val agentsMax = 5
    val agentAddDensity = 1.0
    val agentRemoveDensity = 0.5

    val metrics = createSystemRunAndGetMetrics(lambda, agentAddDensity, agentRemoveDensity, agentsMin, agentsMax, mu)

    val titleSuffix = "l=$lambda,m=$mu,amn=$agentsMin,amx=$agentsMax,+=$agentAddDensity,-=$agentRemoveDensity"

    val amountOfAgentsDynamic = metrics.getDynamic(DYNAMIC_GROUP_AGENTS_AMOUNT_METRIC, "DynamicGroupNode")
    val timeInQueueDynamic = metrics.getDynamic(TIME_IN_SYSTEM_METRIC, "SimpleEvent")
    val eventsInQueueDynamic = metrics.getDynamic(getPrioritizedEventPersistMetricName(), "QueuePersistence")

    plot {
        line {
            x(dayTimeRangeMinutes())
            y(amountOfAgentsDynamic)

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "Amount of agents. $titleSuffix"
            xAxisLabel = "day minutes"
            yAxisLabel = "amount of agents (pts)"
        }
    }.save(
        "fixedVars_amountOfAgents.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )

    plot {
        line {
            x(dayTimeRangeMinutes())
            y(timeInQueueDynamic)

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "Time in queue. $titleSuffix"
            xAxisLabel = "day minutes"
            yAxisLabel = "time in queue (min)"
        }
    }.save(
        "fixedVars_timeInQueue.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )

    plot {
        line {
            x(dayTimeRangeMinutes())
            y(eventsInQueueDynamic)

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "Amount of events in queue. $titleSuffix"
            xAxisLabel = "day minutes"
            yAxisLabel = "events in queue (pts)"
        }
    }.save(
        "fixedVars_eventInQueue.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )
}

private fun plotCase() {
    // Collect data until reach:
    val minPlotAmount = 10

    val range = (1..15)

    val agentsMin = range.first
    val agentsMax = range.last
    val lambda = 10
    val mu = 5
    val agentAddDensity = 1.0
    val agentRemoveDensity = 0.5

    val resultMap: Map<Int, Pair<MutableList<Int>, MutableList<Int>>> =
        range.associate { it to (mutableListOf<Int>() to mutableListOf<Int>()) }

    while (resultMap.all { it.value.first.size < minPlotAmount || it.value.second.size < minPlotAmount}) {
        val metrics = createSystemRunAndGetMetrics(lambda, agentAddDensity, agentRemoveDensity, agentsMin, agentsMax, mu)

        val amountOfAgentsDynamic = metrics.getDynamic(DYNAMIC_GROUP_AGENTS_AMOUNT_METRIC, "DynamicGroupNode")
        val queueSizeDynamic = metrics.getDynamic(getPrioritizedEventPersistMetricName(), "QueuePersistence")
        val timeInSystemDynamic = metrics.getDynamic(TIME_IN_SYSTEM_METRIC, "SimpleEvent")

        val metricZip = queueSizeDynamic.zip(timeInSystemDynamic)
        val zip = amountOfAgentsDynamic.zip(metricZip)

        zip.map { (amountOfAgents, metricPair) ->
            val queueSize = metricPair.first
            val timeInSystem = metricPair.second

            resultMap.getValue(amountOfAgents).first.add(queueSize)
            resultMap.getValue(amountOfAgents).second.add(timeInSystem)
        }
    }

    val toPlot = resultMap.map { it.value.first.take(minPlotAmount).average() to it.value.second.take(minPlotAmount).average() }

    plot {
        line {
            x(range)
            y(toPlot.map { it.first })

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "Queue size"
            xAxisLabel = "amount of agents (pts)"
            yAxisLabel = "events in queue (pts)"
        }
    }.save(
        "agentsAmount_queueSize.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )

    plot {
        line {
            x(range)
            y(toPlot.map { it.second })

            layout {
                color = Color.RED
            }
        }

        layout {
            title = "Time in system"
            xAxisLabel = "amount of agents (pts)"
            yAxisLabel = "time in system (min)"
        }
    }.save(
        "agentsAmount_timeInSystem.png",
        scale = 1.5,
        dpi = 300,
        path = docsDir()
    )
}

private fun tableCase() {
    val table = L5Table()

    val lambda = 10
    val mu = 3
    val agentAddIs = (1..7)
    val agentRemoveIs = (1..7)
    val minAgentAmount = 1
    val maxAgentAmount = 5

    agentAddIs.map { agentAddI ->
        val agentRemoveI = agentRemoveIs.first

        val metrics = createSystemRunAndGetMetrics(
            lambda,
            agentAddI * 0.5,
            agentRemoveI * 0.5,
            minAgentAmount,
            maxAgentAmount,
            mu
        )
        table.append(lambda, mu, minAgentAmount, maxAgentAmount, agentAddI * 0.5, agentRemoveI * 0.5, metrics)
    }

    agentRemoveIs.map { agentRemoveI ->
        val agentAddI = agentRemoveIs.median()

        val metrics = createSystemRunAndGetMetrics(
            lambda,
            agentAddI * 0.5,
            agentRemoveI * 0.5,
            minAgentAmount,
            maxAgentAmount,
            mu
        )
        table.append(lambda, mu, minAgentAmount, maxAgentAmount, agentAddI * 0.5, agentRemoveI * 0.5, metrics)
    }

    table.intoDataFrame().writeCSV(docsDir() + "lab5.csv")
}

private data class L5Table(
    val lambda: MutableList<Int> = mutableListOf(),
    val mu: MutableList<Int> = mutableListOf(),
    val agentsMin: MutableList<Int> = mutableListOf(),
    val agentsMax: MutableList<Int> = mutableListOf(),
    val agentAddLambda: MutableList<Double> = mutableListOf(),
    val agentRemoveLambda: MutableList<Double> = mutableListOf(),
    val averageTimeInSystem: MutableList<Double> = mutableListOf(),
    val averageAmountOfEventsInSystem: MutableList<Double> = mutableListOf(),
) {
    fun append(
        lambda: Int,
        mu: Int,
        agentsMin: Int,
        agentsMax: Int,
        agentAddLambda: Double,
        agentRemoveLambda: Double,
        metrics: Map<String, Counter>,
    ) {
        this.lambda.add(lambda)
        this.mu.add(mu)
        this.agentsMin.add(agentsMin)
        this.agentsMax.add(agentsMax)
        this.agentAddLambda.add(agentAddLambda)
        this.agentRemoveLambda.add(agentRemoveLambda)

        val averageTimeInSystem = metrics.getDynamic(TIME_IN_SYSTEM_METRIC, "SimpleEvent").average()
        val averageAmountOfEventsInSystem = metrics.getDynamic(getPrioritizedEventPersistMetricName(), "QueuePersistence").average()

        this.averageTimeInSystem.add(averageTimeInSystem)
        this.averageAmountOfEventsInSystem.add(averageAmountOfEventsInSystem)
    }

    fun intoDataFrame(): DataFrame<*> {
        return dataFrameOf(
            "lambda" to lambda,
            "mu" to mu,
            "agentsMin" to agentsMin,
            "agentsMax" to agentsMax,
            "agentAddLambda" to agentAddLambda,
            "agentRemoveLambda" to agentRemoveLambda,
            "averageTimeInSystem" to averageTimeInSystem,
            "averageAmountOfEventsInSystem" to averageAmountOfEventsInSystem,
        )
    }
}

fun runL5() {
    fixedVarsCase()
    plotCase()
    tableCase()
}