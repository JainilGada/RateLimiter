package xyz.sarva.ratelimiter.test

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@BenchmarkMode(org.openjdk.jmh.annotations.Mode.Throughput)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
open class CasVsSyncBenchmark {

    private val atomic = AtomicLong(0)
    private var count = 0L
    private val lock = Any()

    @Benchmark
    fun casIncrement(): Long {
        return atomic.getAndIncrement()
    }

    @Benchmark
    fun syncIncrement(): Long {
        synchronized(lock) {
            return count++
        }
    }
}
