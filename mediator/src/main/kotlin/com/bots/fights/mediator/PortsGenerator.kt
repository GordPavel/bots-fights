package com.bots.fights.mediator

import org.springframework.stereotype.Component
import java.io.IOException
import java.net.DatagramSocket
import java.net.ServerSocket
import java.util.Random
import java.util.concurrent.ThreadLocalRandom

private const val MIN_BOUND = 10000
private const val MAX_BOUND = 50000

@Component
class PortsGenerator {
    fun generatePorts(): Pair<Int, Int> {
        val random: Random = ThreadLocalRandom.current()
        val left: Int = generatePort(random, listOf(::available))
        val right: Int = generatePort(random, listOf({ it != left }, ::available))
        return left to right
    }

    private fun generatePort(random: Random, checkers: Collection<(Int) -> Boolean>): Int {
        var port: Int
        while (true) {
            port = random.nextInt(MIN_BOUND, MAX_BOUND)
            if (checkers.all { it(port) }) {
                break
            }
        }
        return port
    }

    private fun available(port: Int): Boolean {
        var ss: ServerSocket? = null
        var ds: DatagramSocket? = null
        try {
            ss = ServerSocket(port)
            ss.reuseAddress = true
            ds = DatagramSocket(port)
            ds.reuseAddress = true
            return true
        } catch (_: IOException) {
        } finally {
            ds?.close()
            if (ss != null) {
                try {
                    ss.close()
                } catch (_: IOException) {
                }
            }
        }
        return false
    }
}