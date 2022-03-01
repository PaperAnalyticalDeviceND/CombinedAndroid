package edu.nd.crc.paperanalyticaldevices.api.utils

import okhttp3.MediaType
import okhttp3.ResponseBody
import okio.*

internal class ProgressResponseBody(
    private val delegate: ResponseBody,
    private val callback: ProgressCallback,
) : ResponseBody() {
    private val source: BufferedSource = Okio.buffer(CountingSource(delegate.source()))

    override fun contentType(): MediaType? = delegate.contentType()
    override fun contentLength(): Long = delegate.contentLength()

    override fun source(): BufferedSource = source

    private inner class CountingSource(delegate: Source) : ForwardingSource(delegate) {
        var uploaded = 0L
        val total = contentLength()
        override fun read(sink: Buffer, byteCount: Long): Long {
            val bytesRead = super.read(sink, byteCount)
            if (bytesRead != -1L) {
                uploaded += bytesRead
                callback.onProgress(uploaded, total)
            }
            return bytesRead
        }
    }
}