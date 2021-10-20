package androidovshchik.tg.sms

import android.content.Context
import android.provider.Telephony.Sms
import androidovshchik.tg.sms.local.Chat
import androidovshchik.tg.sms.local.Preferences
import androidx.work.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.request.SendMessage
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result = with(applicationContext) {
        val chats = db.chatDao().selectAll()
        val preferences = Preferences(applicationContext)
        val minSmsId = (chats.firstOrNull()?.lastSmsId ?: preferences.lastSmsId).toString()
        contentResolver.query(Sms.Inbox.CONTENT_URI, null, "${Sms._ID} >= ?", arrayOf(minSmsId), "${Sms._ID} ASC")?.use { cursor ->
            if (!cursor.moveToLast()) {
                return@with Result.failure()
            }
            val lastSmsId = cursor.getInt(cursor.getColumnIndexOrThrow(Sms._ID)) + 1
            preferences.lastSmsId = lastSmsId
            val token = preferences.botToken?.trim()
            if (token.isNullOrBlank()) {
                return@with Result.failure()
            }
            val bot = TelegramBot(token)
            val code = preferences.authCode.trim()
            var lastUpdateId = preferences.lastUpdateId
            while (true) {
                try {
                    val request = GetUpdates()
                        .limit(100)
                        .offset(lastUpdateId + 1)
                        .timeout(TimeUnit.MINUTES.toMillis(1).toInt())
                    val updates = bot.execute(request).updates()
                    if (updates.isEmpty()) {
                        break
                    }
                    updates.forEach { upd ->
                        if (upd.message().text().trim() == code) {
                            val chatId = upd.message().chat().id()
                            if (!chats.any { it.id == chatId }) {
                                val chat = Chat(chatId, lastSmsId)
                                chats.add(chat)
                                db.chatDao().insert(chat)
                            }
                        }
                    }
                    updates.lastOrNull()?.let {
                        lastUpdateId = it.updateId()
                        preferences.lastUpdateId = lastUpdateId
                    }
                } catch (e: Throwable) {
                    Timber.e(e)
                    break
                }
            }
            for (chat in chats) {
                if (!cursor.moveToFirst()) {
                    break
                }
                do {
                    try {
                        val smsId = cursor.getInt(cursor.getColumnIndexOrThrow(Sms._ID))
                        if (chat.lastSmsId > smsId) {
                            continue
                        }
                        val address = cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS))
                        val body = cursor.getString(cursor.getColumnIndexOrThrow(Sms.BODY))
                        val date = cursor.getLong(cursor.getColumnIndexOrThrow(Sms.DATE))
                        val response = bot.execute(SendMessage(chat.id, """
                            $address $date
                            ```$body```
                        """.trimIndent()))
                        if (response.isOk) {
                            db.chatDao().update(chat.also {
                                it.lastSmsId = smsId + 1
                            })
                        }
                    } catch (e: Throwable) {
                        Timber.e(e)
                        break
                    }
                } while (cursor.moveToNext())
            }
        }
        return Result.success()
    }

    companion object {

        private const val NAME = "Main"

        fun launch(context: Context) {
            val request = OneTimeWorkRequestBuilder<MainWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).run {
                enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, request)
            }
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).apply {
                cancelUniqueWork(NAME)
            }
        }
    }
}