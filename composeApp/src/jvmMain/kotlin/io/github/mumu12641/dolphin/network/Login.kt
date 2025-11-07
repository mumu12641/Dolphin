package io.github.mumu12641.dolphin.network

import io.github.mumu12641.dolphin.network.utils.Decaptcha
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.Url
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.KeyFactory
import java.security.Security
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher

object Login {
    private const val BASE_URL = "https://pass.hust.edu.cn/cas/login"
    private const val RSA_URL = "https://pass.hust.edu.cn/cas/rsa"
    private const val CAPTCHA_URL = "https://pass.hust.edu.cn/cas/code"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    suspend fun login(
        username: String,
        password: String,
        targetUrl: String = "http://pecg.hust.edu.cn/cggl/index1"
    ): Map<String, String> {
        val cookieStorage = AcceptAllCookiesStorage()
        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
            }
            install(HttpCookies) {
                storage = cookieStorage
            }
            followRedirects = false
        }

        return try {
            client.use { safeClient ->
                val loginHtml =
                    safeClient.get(BASE_URL) { parameter("service", targetUrl) }.bodyAsText()

                val (lt, execution) = extractFormParameters(loginHtml)
                val captchaCode = handleCaptcha(safeClient, loginHtml)
                println(captchaCode)
                val (encryptedUsername, encryptedPassword) = getEncryptedCredentials(
                    safeClient,
                    username,
                    password
                )

                val formParameters = Parameters.build {
                    append("rsa", "")
                    append("ul", encryptedUsername)
                    append("pl", encryptedPassword)
                    append("code", captchaCode)
                    append("phoneCode", "")
                    append("lt", lt)
                    append("execution", execution)
                    append("_eventId", "submit")
                }

                val postResponse =
                    safeClient.submitForm(url = BASE_URL, formParameters = formParameters) {
                        parameter("service", targetUrl)
                    }

                postResponse.headers[HttpHeaders.Location]?.let { locationUrl ->
                    safeClient.get(locationUrl)
                }

                safeClient.get(targetUrl)

                cookieStorage.get(Url(targetUrl)).associate { it.name to it.value }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    private suspend fun handleCaptcha(client: HttpClient, loginHtml: String): String {
        val needsCaptcha = "<div class=\"ide-code-box\">" in loginHtml
        if (!needsCaptcha) return ""

        val captchaBytes = client.get(CAPTCHA_URL).readBytes()
        return Decaptcha.decaptcha(captchaBytes).trim()
    }

    private suspend fun getEncryptedCredentials(
        client: HttpClient,
        user: String,
        pass: String
    ): Pair<String, String> {
        val rsaJson = client.post(RSA_URL) {
            contentType(ContentType.Application.Json)
        }.bodyAsText()

        val publicKeyBase64 =
            Json.parseToJsonElement(rsaJson).jsonObject["publicKey"]!!.jsonPrimitive.content
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val publicKey = keyFactory.generatePublic(keySpec)

        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)

        val encryptedUser = Base64.getEncoder().encodeToString(cipher.doFinal(user.toByteArray()))
        val encryptedPass = Base64.getEncoder().encodeToString(cipher.doFinal(pass.toByteArray()))

        return Pair(encryptedUser, encryptedPass)
    }

    private fun extractFormParameters(html: String): Pair<String, String> {
        val ltRegex = Regex("name=\"lt\" value=\"(.*?)\"")
        val executionRegex = Regex("name=\"execution\" value=\"(.*?)\"")

        val lt = ltRegex.find(html)?.groupValues?.get(1) ?: ""
        val execution = executionRegex.find(html)?.groupValues?.get(1) ?: ""
        return Pair(lt, execution)
    }
}