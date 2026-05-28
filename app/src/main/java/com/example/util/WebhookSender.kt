package com.example.util

import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object WebhookSender {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun sendWebhook(url: String, method: String, body: String?): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBuilder = Request.Builder().url(url)
                
                if (method.equals("POST", ignoreCase = true)) {
                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val requestBody = (body ?: "{}").toRequestBody(mediaType)
                    requestBuilder.post(requestBody)
                } else {
                    requestBuilder.get()
                }

                val request = requestBuilder.build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Result.success("Success: Code ${response.code}")
                    } else {
                        Result.failure(IOException("Failed: Code ${response.code} - ${response.message}"))
                    }
                }
            } catch (e: Exception) {
                Log.e("WebhookSender", "Failed to send webhook to $url", e)
                Result.failure(e)
            }
        }
    }
}
