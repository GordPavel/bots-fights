package com.bots.fights.stub

import com.bots.fights.common.Field
import com.bots.fights.common.Figure
import com.bots.fights.common.UNKNOWN
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class DumbGameManager(
    @Value("\${turn.figure}")
    private val turnFigure: Figure,
) : GameManager {

    private val random: Random = Random.Default

    override fun makeTurn(field: Field): Field {
        val freePositions = field.field.withIndex().asSequence().flatMap { (i, row) ->
            row.withIndex().asSequence().flatMap { (j, figure) ->
                if (figure == UNKNOWN) sequenceOf(i to j) else sequenceOf()
            }
        }.toList()
        val nextPosition = random.nextInt(0, freePositions.size)
        val (i, j) = freePositions[nextPosition]
        field.field[i][j] = turnFigure
        return field
    }
}