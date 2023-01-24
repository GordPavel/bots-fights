package com.bots.fights.common

private const val ROWS_SPLITTER = "\n"
private const val ROW_SPLITTER = ","

class FieldConverter {
    fun read(rawField: String): Field = Field(
        field = rawField.split(ROWS_SPLITTER).map { it.split(ROW_SPLITTER).map(::readFigure).toTypedArray() }
            .toTypedArray(),
    )

    fun write(field: Field): String = field.field
        .joinToString(separator = ROWS_SPLITTER) {
            it.joinToString(
                separator = ROW_SPLITTER,
                transform = ::writeFigure
            )
        }

    private fun readFigure(rawFigure: String): Figure = when (rawFigure) {
        "X"  -> X
        "O"  -> O
        else -> UNKNOWN
    }

    private fun writeFigure(figure: Figure): String = when (figure) {
        X       -> "X"
        O       -> "O"
        UNKNOWN -> "-"
    }

}