package com.bots.fights.stub

import com.bots.fights.common.FieldConverter
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController(
    private val fieldConverter: FieldConverter,
    private val gameManager: GameManager,
) {
    @PostMapping("/turn")
    fun makeTurn(
        @RequestBody rawField: String
    ): String {
        var field = fieldConverter.read(rawField)
        field = gameManager.makeTurn(field)
        return fieldConverter.write(field)
    }
}