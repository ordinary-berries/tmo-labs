package me.ordinary_berries.tmo.schema.nodes

interface NodeSupplier {
    fun supplyNodes(nodeConstructor: NodeConstructor): List<Node>
}
