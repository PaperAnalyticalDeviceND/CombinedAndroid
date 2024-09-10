package edu.nd.crc.paperanalyticaldevices.api.utils

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Finds requests tagged with [ProgressCallback] instances, and wraps the request
 * in a [ProgressRequestBody] that will issue updates to this callback.
 */
internal class ProgressInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val progressCallback = request.tag(ProgressCallback::class.java)
        if (progressCallback != null) {
            if (request.body != null) {
                return chain.proceed(wrapRequest(request, progressCallback))
            }

            val response = chain.proceed(request)
            if (response.body != null) {
                return wrapResponse(response, progressCallback)
            }
        }

        return chain.proceed(request)
    }

    private fun wrapRequest(request: Request, progressCallback: ProgressCallback): Request {
        requireNotNull(request.body)

        return request.newBuilder()
            .post(ProgressRequestBody(request.body!!, progressCallback))
            .build()
    }

    private fun wrapResponse(response: Response, progressCallback: ProgressCallback): Response {
        requireNotNull(response.body)

        return response.newBuilder()
            .body(ProgressResponseBody(response.body!!, progressCallback))
            .build()
    }
}
