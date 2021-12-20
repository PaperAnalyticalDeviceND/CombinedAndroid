package edu.nd.crc.paperanalyticaldevices.api.utils

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.*
import kotlin.math.min

internal class ProgressRequestBody(
    private val delegate: RequestBody,
    private val callback: ProgressCallback
) : RequestBody() {
    companion object {
        const val BYTES_PER_CHUNK = 16L * 1024;
    }

    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = Okio.buffer(CountingSink(sink))
        delegate.writeTo(countingSink)
        countingSink.flush()
    }

    private inner class CountingSink(delegate: Sink) : ForwardingSink(delegate) {
        val total = contentLength()
        var uploaded = 0L
        override fun write(source: Buffer, byteCount: Long) {
            var remaining = byteCount;
            while (remaining > 0) {
                val toSend = min(remaining, Companion.BYTES_PER_CHUNK)
                super.write(source, toSend)
                remaining -= toSend;

                uploaded += toSend
                callback.onProgress(uploaded, total)
            }
        }
    }
}