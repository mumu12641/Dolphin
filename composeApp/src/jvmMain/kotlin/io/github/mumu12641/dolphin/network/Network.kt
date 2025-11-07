package io.github.mumu12641.dolphin.network

import io.github.mumu12641.dolphin.model.BookingInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.regex.Pattern

object Network {
    suspend fun testConnection(
        cookies: Map<String, String>,
        bookingInfo: BookingInfo
    ): Result<Pair<String, String>> {
        println(bookingInfo)
        try {
            val client = OkHttpClient()

            val testUrls = listOf(
                "https://pecg.hust.edu.cn/cggl/front/syqk?cdbh=${bookingInfo.venueId}&date=${bookingInfo.orderDate}&starttime=${bookingInfo.startTime}&endtime=${bookingInfo.endTime}",
                "https://pecg.hust.edu.cn/cggl/front/yuyuexz",
            )

            // 构建Cookie字符串
            val cookieString = cookies.entries.joinToString("; ") { (key, value) -> "$key=$value" }

            for (attempt in testUrls.indices) {
                val testUrl = testUrls[attempt]
                val request = Request.Builder()
                    .url(testUrl)
                    .addHeader(
                        "Accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
                    )
                    // 不添加Accept-Encoding以避免自动解压缩问题
                    // .addHeader("Accept-Encoding", "gzip, deflate")
                    .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                    .addHeader("Cache-Control", "max-age=0")
                    .addHeader("Connection", "keep-alive")
                    .addHeader("Host", "pecg.hust.edu.cn")
                    .addHeader("Referer", "http://pecg.hust.edu.cn/cggl/front/syqk?cdbh=${bookingInfo.venueId}")
                    .addHeader("Upgrade-Insecure-Requests", "1")
                    .addHeader(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
                    )
                    .addHeader("Cookie", cookieString)
                    .get()
                    .build()

                println(testUrl)

                client.newCall(request).execute().use { response ->
                    println(response)
                    println("Response headers: ${response.headers}")

                    val setCookieHeaders = response.headers("Set-Cookie")
                    println("Set-Cookie headers: $setCookieHeaders")
                    setCookieHeaders.forEach { setCookie ->
                        println("Set-Cookie: $setCookie")
                    }

                    if (response.isSuccessful) {
                        val responseText = response.body?.string() ?: ""
                        println(responseText)

                        val csrfTokenMatcher =
                            Pattern.compile("cg_csrf_token\\\"\\s+value=\\\"([a-f0-9-]+)\\\"")
                                .matcher(responseText)
                        val cgCsrfToken =
                            if (csrfTokenMatcher.find()) csrfTokenMatcher.group(1) else null

                        val token = if (attempt == 0) {
                            val tokenMatcher =
                                Pattern.compile("\\\"token\\\":\\s*\\\"([a-f0-9]+)\\\"")
                                    .matcher(responseText)
                            if (tokenMatcher.find()) tokenMatcher.group(1) else null
                        } else {
                            val scriptPattern =
                                """<script type="text/javascript">.*?name=\\"token\\".*?value=\\"([a-f0-9]{32})\\".*?</script>"""
                                    .toRegex(RegexOption.DOT_MATCHES_ALL)
                            scriptPattern.find(responseText)?.groupValues?.get(1)
                        }

                        if (cgCsrfToken == null || token == null)
                            return Result.Error(Exception("Token all none"))
                        return Result.Success(Pair(cgCsrfToken, token))
                    }
                }
            }
            return Result.Error(Exception("Token not found"))
        } catch (e: Exception) {
            return Result.Error(e)
        }
    }
}