package com.bots.fights.stub

import com.bots.fights.common.FieldConverter
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class StubGameServiceApplication {
    @Bean
    fun fieldConverter() = FieldConverter()
}

fun main(args: Array<String>) {
    runApplication<StubGameServiceApplication>(*args)
}
