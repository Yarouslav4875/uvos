package com.uvos.terminal.core

import java.io.InputStream
import java.io.OutputStream

class ByteQueue(private val capacity: Int = 32768) {
    private val buffer = ByteArray(capacity)
    private var head = 0
    private var tail = 0
    private var count = 0
    private val lock = java.lang.Object()

    fun write(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): Int {
        synchronized(lock) {
            var written = 0
            while (written < length) {
                while (count == capacity) {
                    lock.wait()
                }
                val canWrite = minOf(length - written, capacity - count)
                val firstChunk = minOf(canWrite, capacity - tail)
                System.arraycopy(bytes, offset + written, buffer, tail, firstChunk)
                tail = (tail + firstChunk) % capacity

                val remaining = canWrite - firstChunk
                if (remaining > 0) {
                    System.arraycopy(bytes, offset + written + firstChunk, buffer, tail, remaining)
                    tail = (tail + remaining) % capacity
                }

                count += canWrite
                written += canWrite
                lock.notifyAll()
            }
            return written
        }
    }

    fun read(target: ByteArray, offset: Int = 0, length: Int = target.size): Int {
        synchronized(lock) {
            while (count == 0) {
                lock.wait()
            }
            val canRead = minOf(length, count)
            val firstChunk = minOf(canRead, capacity - head)
            System.arraycopy(buffer, head, target, offset, firstChunk)
            head = (head + firstChunk) % capacity

            val remaining = canRead - firstChunk
            if (remaining > 0) {
                System.arraycopy(buffer, head, target, offset + firstChunk, remaining)
                head = (head + remaining) % capacity
            }

            count -= canRead
            lock.notifyAll()
            return canRead
        }
    }

    fun available(): Int = synchronized(lock) { count }

    fun clear() = synchronized(lock) {
        head = 0
        tail = 0
        count = 0
        lock.notifyAll()
    }
}
