package com.bots.fights.common

data class Field(
    val field: Array<Array<Figure>>,
)

sealed interface Figure {
    fun value(): String
    fun smallValue(): String
}

object X : Figure {
    override fun value(): String = "X"
    override fun smallValue(): String = "x"
}

object O : Figure {
    override fun value(): String = "O"
    override fun smallValue(): String = "o"
}

object UNKNOWN : Figure {
    override fun value(): String = "-"
    override fun smallValue(): String = "-"
}
