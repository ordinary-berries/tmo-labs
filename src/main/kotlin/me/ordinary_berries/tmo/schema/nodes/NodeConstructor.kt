package me.ordinary_berries.tmo.schema.nodes

interface NodeConstructor {
    fun createNode(): Node

    fun batchCreateNodes(amount: Int): List<Node>
}
