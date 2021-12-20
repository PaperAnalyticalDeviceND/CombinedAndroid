package edu.nd.crc.paperanalyticaldevices.api.utils

/**
 * Callback to listen for file upload status.
 */
interface ProgressCallback {

    /**
     * Called when the attachment is uploaded successfully with an [url].
     */
    fun onSuccess(url: String?)
    /**
     * Called when the attachment upload is in progress with [bytes] count
     * and [totalBytes] in bytes of the file.
     */
    fun onProgress(bytes: Long, totalBytes: Long)
}
