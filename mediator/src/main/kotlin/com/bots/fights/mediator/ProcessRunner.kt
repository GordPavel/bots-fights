package com.bots.fights.mediator

import com.bots.fights.common.Figure
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LogLevel
import org.springframework.boot.logging.LogLevel.DEBUG
import org.springframework.boot.logging.LogLevel.ERROR
import org.springframework.boot.logging.LogLevel.INFO
import org.springframework.boot.logging.LogLevel.TRACE
import org.springframework.boot.logging.LogLevel.WARN
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpMethod
import org.springframework.web.client.RestTemplate
import java.io.InputStream

private const val READINESS_PATH = "/system/health/readiness"
private const val SUCCESS_READINESS_RESPONSE = "{\"status\":\"UP\"}"
private const val SUCCESS_READINESS_TEST = "SUCCESS"
private const val ERROR_READINESS_TEST = "ERROR"
private const val LOG_FLUSHER_DELAY: Long = 50
private const val READINESS_SLEEP: Long = 50

class ProcessRunner(
    side: String,
    private val figure: Figure,
    private val port: Int,
    private val jarPath: String,
    private val javaLocation: String,
    private val commonJvmPropertiesTemplate: String,
    private val commonPropertiesTemplate: String,
) {

    private val runtime = Runtime.getRuntime()

    private val processLogger = LoggerFactory.getLogger("$side.logger")

    val restClient: RestTemplate = RestTemplateBuilder()
        .rootUri("http://localhost:$port")
        .build()

    suspend fun startProcess(): Process = coroutineScope {
        val process = internalStartProcess()
        processLogger.debug("Started with ${process.pid()} PID on port $port")
        logJob(process)
        return@coroutineScope process
    }

    private suspend fun internalStartProcess(): Process = coroutineScope {
        val springProperties = commonPropertiesTemplate.format(port, figure.value())
        withContext(IO) { runtime.exec("$javaLocation $commonJvmPropertiesTemplate $springProperties -jar $jarPath") }
    }

    private suspend fun logJob(process: Process): Job = coroutineScope {
        launch { streamFlusher(process.inputStream, TRACE) }
        launch { streamFlusher(process.errorStream, WARN) }
    }

    private suspend fun streamFlusher(stream: InputStream, level: LogLevel) = coroutineScope {
        while (this.isActive) {
            val available = withContext(IO) { stream.available() }
            if (available <= 0) continue
            String(withContext(IO) { stream.readNBytes(available) }).split("\n").forEach {
                when (level) {
                    ERROR -> processLogger.error(it)
                    WARN  -> processLogger.warn(it)
                    DEBUG -> processLogger.debug(it)
                    INFO  -> processLogger.info(it)
                    TRACE -> processLogger.trace(it)
                    else  -> {}
                }
            }
            delay(LOG_FLUSHER_DELAY)
        }
    }

    suspend fun waitStart() {
        var readinessWaiterCounter = 0
        while (true) {
            val result = withContext(IO) {
                val response = try {
                    restClient.exchange(
                        READINESS_PATH,
                        HttpMethod.GET,
                        null,
                        String::class.java,
                    )
                } catch (_: Throwable) {
                    return@withContext ERROR_READINESS_TEST
                }
                if (response.statusCode.is2xxSuccessful && response.body == SUCCESS_READINESS_RESPONSE) {
                    return@withContext SUCCESS_READINESS_TEST
                } else {
                    return@withContext ERROR_READINESS_TEST
                }
            }
            if (SUCCESS_READINESS_TEST == result) {
                break
            }
            delay(READINESS_SLEEP)
            readinessWaiterCounter++
        }
        processLogger.debug("Left was starting for ${readinessWaiterCounter * READINESS_SLEEP}ms")
    }

}