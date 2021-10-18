package androidovshchik.tg.sms

import android.content.Context
import android.database.DatabaseUtils
import android.provider.Telephony.Sms
import androidovshchik.tg.sms.ext.longBgToast
import androidovshchik.tg.sms.local.Preferences
import androidx.work.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.GetUpdates
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result = applicationContext.run {
        val chats = db.chatDao().selectAll()
        val preferences = Preferences(applicationContext)
        val minSmsId = (chats.firstOrNull()?.lastSmsId ?: preferences.lastSmsId).toString()
        contentResolver.query(Sms.Inbox.CONTENT_URI, null, "${Sms._ID} > ?", arrayOf(minSmsId), "${Sms._ID} ASC")?.use {
            if (it.moveToLast()) {
                preferences.lastSmsId = it.getInt(it.getColumnIndexOrThrow(Sms._ID))
                val token = preferences.botToken?.trim()
                if (token.isNullOrBlank()) {
                    applicationContext.longBgToast("Не задан токен бота")
                    return Result.retry()
                }
                val code = preferences.authCode
                val bot = TelegramBot(token)
                val getUpdates = GetUpdates()
                    .limit(100)
                    .offset(preferences.lastUpdateId + 1)
                    .timeout(TimeUnit.MINUTES.toMillis(5).toInt())
                val updates = bot.execute(getUpdates).updates()
                updates.forEach {
                    if (it.message().text().trim() == code) {
                        preferences.allowedChats.add(it.message().chat().id().toString())
                    }
                }
                updates.lastOrNull()?.let {
                    preferences.lastUpdateId = it.updateId()
                }
                it.moveToFirst()
                while (it.moveToNext()) {
                    Timber.e(DatabaseUtils.dumpCursorToString(it))
                }
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