package com.bots.fights.mediator

import com.bots.fights.common.Field
import com.bots.fights.common.FieldConverter
import com.bots.fights.common.Figure
import com.bots.fights.common.UNKNOWN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.time.Duration

private const val TURN_ENDPOINT = "/turn"

class GameController(
    private val leftClient: RestTemplate,
    private val rightClient: RestTemplate,
    private val turnDelay: Duration,
    private val fieldSize: Int,
) {

    private val logger = LoggerFactory.getLogger("Game.logger")

    private val fieldConverter = FieldConverter()

    private var field = Field(
        generateSequence {
            generateSequence<Figure> { UNKNOWN }.take(fieldSize).toList().toTypedArray()
        }
            .take(fieldSize)
            .toList()
            .toTypedArray()
    )

    suspend fun runGame() {
        printField(field)
        while (!isGameOver(field)) {
            makeTurn(leftClient)
            if (isGameOver(field)) break
            makeTurn(rightClient)
        }
        printField(field)
    }

    private suspend fun makeTurn(client: RestTemplate) {
        val response = withContext(Dispatchers.IO) {
            client.exchange(
                TURN_ENDPOINT,
                HttpMethod.POST,
                HttpEntity<String>(fieldConverter.write(field)),
                String::class.java,
            )
        }
        val newField = fieldConverter.read(response.body!!)
        printField(field, newField)
        delay(turnDelay.toMillis())
    }

    private fun isGameOver(field: Field): Boolean = field.field.all { row -> row.all { it != UNKNOWN } }

    private fun printField(
        previous: Field,
        current: Field? = null,
    ) {
        if (current == null) {
            previous.field.forEach { row ->
                logger.info(row.joinToString(separator = ",", transform = Figure::smallValue))
            }
        } else {
            var changeField = true
            previous.field.zip(current.field).withIndex().forEach { (i, rows) ->
                val (prevRow, currentRow) = rows
                logger.info(prevRow.zip(currentRow).withIndex().joinToString(separator = ",") { (j, figures) ->
                    val (prevF, currentF) = figures
                    if (prevF == currentF) {
                        return@joinToString prevF.smallValue()
                    }
                    if (prevF == UNKNOWN)
                        return@joinToString currentF.value()

                    logger.warn("INCORRECT TURN, ($i,$j) ${prevF.smallValue()} -> ${currentF.value()}, ")
                    changeField = false
                    return@joinToString prevF.smallValue()

                })
            }
            if (changeField) this.field = current
        }
        logger.info("========================================")
    }

}