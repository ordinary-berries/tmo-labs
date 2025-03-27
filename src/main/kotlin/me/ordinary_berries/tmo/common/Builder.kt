package me.ordinary_berries.tmo.common

import me.ordinary_berries.tmo.build.BuilderContext

interface Builder<B> {
    fun build(): B
    fun getContext(): BuilderContext
}