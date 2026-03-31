package exspecs.concurrency

import org.testng.Assert.*
import org.testng.annotations.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class SyncChannelTest {

    @Test
    fun test1Channel1Sync() {
        businessLogic1Channel(1, 1000)
    }

    @Test
    fun test1Channel2Sync() {
        businessLogic1Channel(2, 1000)
    }

    @Test
    fun test1Channel3Sync() {
        businessLogic1Channel(3, 999)
    }

    @Test
    fun test1Channel4Sync() {
        businessLogic1Channel(4, 1000)
    }

    private fun businessLogic1Channel(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        exspecs.tools.assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val threads = mutableListOf<Thread>()
        for (i in 1.. numThreads) {
            val t = Thread {
                val syncResult = chan.sync()
                // keep track of how many threads have sync'ed on this value
                results.compute(syncResult.result.get(), chmResultUpdate)
            }
            t.start()
            threads.add(t)
        }
        threads.forEach { it.join() }

        // testing
        val maxKey = numThreads / syncSize
        assertEquals(results.size, maxKey)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(results[i], syncSize)
        }
    }

    @Test
    fun test2Channels1Sync() {
        businessLogic2Channels(1, 1000)
    }

    @Test
    fun test2Channels2Sync() {
        businessLogic2Channels(2, 1000)
    }

    @Test
    fun test2Channels3Sync() {
        businessLogic2Channels(3, 999)
    }

    @Test
    fun test2Channels4Sync() {
        businessLogic2Channels(4, 1000)
    }

    private fun businessLogic2Channels(syncSize : Int, numThreads : Int) {
        // if this is false then some threads will hang
        exspecs.tools.assert(numThreads % syncSize == 0)

        val incVal = AtomicInteger(1)
        val results = ConcurrentHashMap<Int,Int>() // value -> count
        val chan1 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val chan2 = SyncChannel<Int,Int>(syncSize) { Optional.of(incVal.getAndIncrement()) }
        val threads = mutableListOf<Thread>()
        for (i in 1.. numThreads) {
            val t1 = Thread {
                val syncResult = chan1.sync()
                results.compute(syncResult.result.get(), chmResultUpdate)
            }
            val t2 = Thread {
                val syncResult = chan2.sync()
                results.compute(syncResult.result.get(), chmResultUpdate)
            }
            t1.start()
            t2.start()
            threads.add(t1)
            threads.add(t2)
        }
        threads.forEach { it.join() }

        // testing
        val maxKey = 2*numThreads / syncSize
        assertEquals(results.size, maxKey)
        for (i in 1..maxKey) {
            assertTrue(results.containsKey(i))
            assertEquals(results[i], syncSize)
        }
    }

    private val chmResultUpdate : (Int,Int?)->Int? = {
        _, curVal ->
            if (curVal == null) {
                1
            } else {
                curVal + 1
            }
    }

    @Test
    fun testClose1() {
        for (i in 1.. 1000) {
            val chan = SyncChannel<Unit,Unit>(2) { Optional.empty() }
            val t1 = Thread {
                chan.sync()
            }
            t1.start()
            assertTrue(t1.isAlive)
            chan.close()
            t1.join(100)
            assertFalse(t1.isAlive)
        }
    }

    @Test
    fun testClose2() {
        for (i in 1.. 20) {
            val chan = SyncChannel<Int,Int>(2) { Optional.of(0) }
            val t1 = Thread { chan.sync() }
            val t2 = Thread { chan.sync() }
            val t3 = Thread { chan.sync() }
            t1.start()
            t2.start()
            t3.start()
            t1.join(100)
            t2.join(100)
            t3.join(100)
            // two threads will have synced, so exactly one must be alive
            assertTrue(t1.isAlive || t2.isAlive || t3.isAlive)
            assertTrue((!t1.isAlive && !t2.isAlive) || (!t1.isAlive && !t3.isAlive) || (!t2.isAlive && !t3.isAlive))

            chan.close()
            t1.join(100)
            t2.join(100)
            t3.join(100)
            assertFalse(t1.isAlive)
            assertFalse(t2.isAlive)
            assertFalse(t3.isAlive)
        }
    }

    @Test
    fun testInterrupt1() {
        for (i in 1.. 1000) {
            val chan = SyncChannel<Unit,Unit>(2) { Optional.empty() }
            val t1 = Thread {
                chan.sync()
            }
            t1.start()
            assertTrue(t1.isAlive)
            t1.interrupt()
            t1.join(100)
            assertFalse(t1.isAlive)
        }
    }

    @Test
    fun testInterrupt2() {
        for (i in 1.. 20) {
            val chan = SyncChannel<Int,Int>(2) { Optional.of(0) }
            val t1 = Thread { chan.sync() }
            val t2 = Thread { chan.sync() }
            val t3 = Thread { chan.sync() }
            t1.start()
            t2.start()
            t3.start()
            t1.join(100)
            t2.join(100)
            t3.join(100)
            // two threads will have synced, so exactly one must be alive
            assertTrue(t1.isAlive || t2.isAlive || t3.isAlive)
            assertTrue((!t1.isAlive && !t2.isAlive) || (!t1.isAlive && !t3.isAlive) || (!t2.isAlive && !t3.isAlive))

            t1.interrupt()
            t2.interrupt()
            t3.interrupt()
            t1.join(100)
            t2.join(100)
            t3.join(100)
            assertFalse(t1.isAlive)
            assertFalse(t2.isAlive)
            assertFalse(t3.isAlive)
        }
    }
}