package androidovshchik.tg.sms

import android.content.Context
import androidovshchik.tg.sms.ext.longBgToast
import androidovshchik.tg.sms.local.Chat
import androidovshchik.tg.sms.local.Preferences
import androidx.work.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetUpdates
import timber.log.Timber

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
        val bot = TelegramBot(token)
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
                    if (upd.message()?.text()?.trim() == code) {
                        try {
                            val tgChat = upd.message().chat()
                            val myChat = Chat(tgChat.id(), lastMsgId + 1)
                            db.chatDao().insert(myChat)
                            Timber.d(myChat.toString())
                            chatNames.add(tgChat.username())
                        } catch (e: Throwable) {
                            Timber.e(e)
                        }
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

    companion object {

        private const val NAME = "Update"

        private const val UPD_LIMIT = 100

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