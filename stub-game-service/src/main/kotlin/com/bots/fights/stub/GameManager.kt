package com.bots.fights.stub

import com.bots.fights.common.Field

interface GameManager {
    fun makeTurn(field: Field): Field
}