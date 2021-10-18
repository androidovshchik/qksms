package androidovshchik.tg.sms

import android.content.Context
import android.provider.Telephony.Sms
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
        contentResolver.query(Sms.Inbox.CONTENT_URI, null, "${Sms._ID} >= ?", arrayOf(minSmsId), "${Sms._ID} ASC")?.use {
            if (!it.moveToLast()) {
                return@with Result.success()
            }
            preferences.lastSmsId = it.getInt(it.getColumnIndexOrThrow(Sms._ID)) + 1
            val token = preferences.botToken?.trim()
            if (token.isNullOrBlank()) {
                return@with Result.success()
            }
            val bot = TelegramBot(token)
            while (true) {
                try {
                    val request = GetUpdates()
                        .limit(100)
                        .offset(preferences.lastUpdateId + 1)
                        .timeout(TimeUnit.MINUTES.toMillis(1).toInt())
                    val updates = bot.execute(request).updates()
                    if (updates.isEmpty()) {
                        break
                    }
                    val code = preferences.authCode
                    updates.forEach {
                        if (it.message().text().trim() == code) {
                            //preferences.allowedChats.add(it.message().chat().id().toString())
                        }
                    }
                    updates.lastOrNull()?.let {
                        preferences.lastUpdateId = it.updateId()
                    }
                } catch (e: Throwable) {
                    Timber.e(e)
                }
            }
            for (chat in chats) {
                if (!it.moveToFirst()) {
                    break
                }
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(Sms._ID))
                    if (chat.lastSmsId > id) {
                        continue
                    }
                    try {
                        val address = it.getString(it.getColumnIndexOrThrow(Sms.ADDRESS))
                        val body = it.getString(it.getColumnIndexOrThrow(Sms.BODY))
                        val date = it.getLong(it.getColumnIndexOrThrow(Sms.DATE))
                        val response = bot.execute(SendMessage(chat.id, """
                            $address $date
                            ```$body```
                        """.trimIndent()))
                        if (response.isOk) {
                            db.chatDao().update(chat.apply {
                                lastSmsId = id + 1
                            })
                        }
                    } catch (e: Throwable) {
                        Timber.e(e)
                        break
                    }
                } while (it.moveToNext())
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