package com.manishraj.saavnmusic.data.remote

import com.manishraj.saavnmusic.core.common.JIOSAAVN_API_ENDPOINT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single transport for everything upstream, replicating the
 * jiosaavn-api repository's `useFetch` helper (UPSTREAM_SPEC §1):
 * one GET endpoint (`api.php`) multiplexed by `__call`, fixed params
 * `_format=json&_marker=0&api_version=4`, a context (`web6dot0` by
 * default, `android` for radio) and a rotating real-browser
 * User-Agent. No cookies, no auth, no tokens.
 *
 * Upstream signals many errors as HTTP 200 with an error/empty body,
 * so callers must validate the expected keys after parsing — this
 * client only guarantees a parsed JSON body on HTTP success.
 */
@Singleton
class JioSaavnClient
    @Inject
    constructor(
        private val http: OkHttpClient,
        private val json: Json,
    ) {
        /** Performs one upstream call and returns the parsed body. Throws [IOException] on transport/HTTP failure. */
        suspend fun callElement(
            endpoint: String,
            callName: String,
            params: Map<String, String> = emptyMap(),
            ctx: String = WEB_CTX,
        ): JsonElement =
            withContext(Dispatchers.IO) {
                val base = endpoint.trim().toHttpUrlOrNull() ?: JIOSAAVN_API_ENDPOINT.toHttpUrlOrNull()
                    ?: throw IOException("Invalid API endpoint")
                val builder = base.newBuilder()
                if (!base.encodedPath.endsWith("api.php")) {
                    builder.addPathSegment("api.php")
                }
                builder
                    .addQueryParameter("__call", callName)
                    .addQueryParameter("_format", "json")
                    .addQueryParameter("_marker", "0")
                    .addQueryParameter("api_version", "4")
                    .addQueryParameter("ctx", ctx)
                params.forEach { (key, value) -> builder.addQueryParameter(key, value) }
                val request =
                    Request
                        .Builder()
                        .url(builder.build())
                        .header("User-Agent", USER_AGENTS.random())
                        .header("Content-Type", "application/json")
                        .get()
                        .build()
                http.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException("Upstream HTTP ${response.code}")
                    }
                    if (body.isBlank()) {
                        throw IOException("Empty upstream response")
                    }
                    json.parseToJsonElement(body)
                }
            }

        /** Like [callElement] but requires a JSON object body. */
        suspend fun callObject(
            endpoint: String,
            callName: String,
            params: Map<String, String> = emptyMap(),
            ctx: String = WEB_CTX,
        ): JsonObject {
            val element = callElement(endpoint, callName, params, ctx)
            return element as? JsonObject ?: throw IOException("Unexpected upstream response shape")
        }

        companion object {
            const val WEB_CTX = "web6dot0"
            const val ANDROID_CTX = "android"

            // A small rotating pool of real browser user agents, like the
            // repository's ~100-entry pool (fetch.helper picks one at random).
            private val USER_AGENTS =
                listOf(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0",
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:131.0) Gecko/20100101 Firefox/131.0",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Safari/605.1.15",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.58 Mobile Safari/537.36",
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.6 Mobile/15E148 Safari/604.1",
                )
        }
    }
