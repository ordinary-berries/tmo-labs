package me.ordinary_berries.tmo.schema.nodes.impl

import me.ordinary_berries.tmo.schema.nodes.Node
import me.ordinary_berries.tmo.schema.nodes.NodeConstructor
import me.ordinary_berries.tmo.schema.nodes.NodeSupplier
import me.ordinary_berries.tmo.util.math.poissonDistribution

class PoissonNodeSupplier(
    private val lambda: Double,
) : NodeSupplier {
    override fun supplyNodes(nodeConstructor: NodeConstructor): List<Node> {
        return nodeConstructor.batchCreateNodes(poissonDistribution(lambda))
    }
}