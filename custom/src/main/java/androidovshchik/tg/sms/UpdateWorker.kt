package androidovshchik.tg.sms

import android.annotation.SuppressLint
import android.content.Context
import androidovshchik.tg.sms.ext.longBgToast
import androidovshchik.tg.sms.local.Chat
import androidovshchik.tg.sms.local.Preferences
import androidx.work.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetUpdates
import okhttp3.OkHttpClient
import timber.log.Timber
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class UpdateWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result = with(applicationContext) {
        val preferences = Preferences(applicationContext)
        val token = preferences.botToken?.trim()
        if (token.isNullOrBlank()) {
            Timber.w("Bot token is not set")
            longBgToast("Не задан токен бота")
            return@with Result.failure()
        }
        var hasErrors = false
        val chatNames = mutableListOf<String?>()
        val bot = TelegramBot.Builder(token)
            .okHttpClient(httpClient)
            .build()
        val code = preferences.authCode.trim()
        var lastUpdateId = preferences.lastUpdateId
        Timber.d("Init lastUpdateId is $lastUpdateId")
        while (true) {
            try {
                val request = GetUpdates()
                    .limit(UPD_LIMIT)
                    .offset(lastUpdateId + 1)
                val updates = bot.execute(request).updates()
                if (updates.isEmpty()) {
                    Timber.d("No more telegram updates")
                    break
                }
                val lastMsgId = db.messageDao().getLastId() ?: -1L
                Timber.d("Now lastMsgId is $lastMsgId")
                updates.forEach { upd ->
                    Timber.d(upd.toString())
                    try {
                        if (upd.message()?.text()?.trim() == code) {
                            val tgChat = upd.message().chat()
                            val myChat = Chat(tgChat.id(), lastMsgId + 1)
                            db.chatDao().insert(myChat)
                            Timber.d(myChat.toString())
                            chatNames.add(tgChat.username())
                        }
                    } catch (e: Throwable) {
                        Timber.e(e)
                    }
                }
                updates.lastOrNull()?.let {
                    lastUpdateId = it.updateId()
                    preferences.lastUpdateId = lastUpdateId
                    Timber.d("New lastUpdateId is $lastUpdateId")
                }
            } catch (e: Throwable) {
                Timber.e(e)
                hasErrors = true
                break
            }
        }
        longBgToast("""
            Добавлены чаты: $chatNames.
            ${if (hasErrors) "Ошибка(и) при обновлении" else "Без ошибок"}
        """.trimIndent())
        return if (hasErrors) Result.failure() else Result.success()
    }

    @SuppressLint("TrustAllX509TrustManager")
    companion object {

        private const val NAME = "Update"

        private const val UPD_LIMIT = 100

        private val httpClient: OkHttpClient

        init {
            val trustAllCerts = arrayOf(object : X509TrustManager {

                override fun checkClientTrusted(chain: Array<out X509Certificate?>?, authType: String?) {}

                override fun checkServerTrusted(chain: Array<out X509Certificate?>?, authType: String?) {}

                override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
            })
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            httpClient = OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0])
                .hostnameVerifier { _, _ -> true }
                .build()
        }

        fun launch(context: Context) {
            val request = OneTimeWorkRequestBuilder<UpdateWorker>()
                .build()
            with(WorkManager.getInstance(context)) {
                enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
            }
        }

        fun cancel(context: Context) {
            with(WorkManager.getInstance(context)) {
                cancelUniqueWork(NAME)
            }
        }
    }
}