package com.bots.fights.mediator

import com.bots.fights.common.O
import com.bots.fights.common.X
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.Duration

private const val LEFT_JAR_LOCATION = "-ljl"
private const val RIGHT_JAR_LOCATION = "-rjl"

@SpringBootApplication
class MediatorApplication(
    private val portsGenerator: PortsGenerator,
) : CommandLineRunner {

    @Value("\${java.location}")
    lateinit var javaLocation: String

    @Value("\${java.configs}")
    lateinit var commonJvmPropertiesTemplate: String

    @Value("\${java.spring}")
    lateinit var commonPropertiesTemplate: String

    @Value("\${game.turn.delay}")
    lateinit var turnDelay: Duration

    @Value("\${game.field.size}")
    lateinit var fieldSize: Integer

    override fun run(vararg args: String) {
        val arguments = args.asSequence()
            .zipWithNext()
            .withIndex()
            .filter { (index, _) -> index % 2 == 0 }
            .associate { (_, v) -> v }
        val leftJar: String = arguments[LEFT_JAR_LOCATION]!!
        val rightJar: String = arguments[RIGHT_JAR_LOCATION]!!
        val (leftPort, rightPort) = portsGenerator.generatePorts()
        runBlocking {
            val leftProcessRunner = ProcessRunner(
                "Left",
                X,
                leftPort,
                leftJar,
                javaLocation,
                commonJvmPropertiesTemplate,
                commonPropertiesTemplate,
            )
            val rightProcessRunner = ProcessRunner(
                "Right",
                O,
                rightPort,
                rightJar,
                javaLocation,
                commonJvmPropertiesTemplate,
                commonPropertiesTemplate,
            )
            var leftProcess: Process? = null
            var leftJob: Job? = null
            var rightProcess: Process? = null
            var rightJob: Job? = null
            try {
                leftJob = launch { leftProcess = leftProcessRunner.startProcess() }
                val leftWaiter = async { leftProcessRunner.waitStart() }
                rightJob = launch { rightProcess = rightProcessRunner.startProcess() }
                val rightWaiter = async { rightProcessRunner.waitStart() }
                leftWaiter.await()
                rightWaiter.await()

                val leftClient = leftProcessRunner.restClient
                val rightClient = rightProcessRunner.restClient

                val gameController = GameController(leftClient, rightClient, turnDelay, fieldSize.toInt())

                gameController.runGame()
            } finally {
                leftJob?.cancel()
                rightJob?.cancel()
                leftProcess?.destroy()
                rightProcess?.destroy()
            }
        }
    }

}

fun main(args: Array<String>) {
    runApplication<MediatorApplication>(*args)
}
