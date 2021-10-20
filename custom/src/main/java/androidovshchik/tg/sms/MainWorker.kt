package androidovshchik.tg.sms

import android.content.Context
import android.provider.Telephony.Sms
import androidovshchik.tg.sms.local.Chat
import androidovshchik.tg.sms.local.Preferences
import androidx.work.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.GetUpdates
import com.pengrad.telegrambot.request.SendMessage
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.util.concurrent.TimeUnit

class MainWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result = with(applicationContext) {
        var lastSmsId = inputData.getLong(PARAM_SMS_ID, 0L)
        val preferences = Preferences(applicationContext)
        val chats = db.chatDao().selectAll()
        Timber.d(chats.toString())
        val minSmsId = (chats.firstOrNull()?.lastSmsId ?: lastSmsId).toString()
        contentResolver.query(Sms.Inbox.CONTENT_URI, null, "${Sms._ID} >= ?", arrayOf(minSmsId), "${Sms._ID} ASC")?.use { cursor ->
            if (cursor.moveToLast()) {
                lastSmsId = cursor.getLong(cursor.getColumnIndexOrThrow(Sms._ID))
            }
            val token = preferences.botToken?.trim()
            if (token.isNullOrBlank()) {
                return@with Result.failure()
            }
            val bot = TelegramBot(token)
            val code = preferences.authCode.trim()
            var lastUpdateId = preferences.lastUpdateId
            Timber.d("Init lastUpdateId is $lastUpdateId")
            while (true) {
                try {
                    val request = GetUpdates()
                        .limit(100)
                        .offset(lastUpdateId + 1)
                    val updates = bot.execute(request).updates()
                    if (updates.isEmpty()) {
                        Timber.d("No new messages was found")
                        break
                    }
                    updates.forEach { upd ->
                        Timber.d(upd.toString())
                        if (upd.message().text().trim() == code) {
                            val chatId = upd.message().chat().id()
                            if (!chats.any { it.id == chatId }) {
                                val chat = Chat(chatId, lastSmsId)
                                chats.add(chat)
                                Timber.d(chat.toString())
                                db.chatDao().insert(chat)
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
                    break
                }
            }
            for (chat in chats) {
                if (!cursor.moveToFirst()) {
                    break
                }
                do {
                    try {
                        val smsId = cursor.getLong(cursor.getColumnIndexOrThrow(Sms._ID))
                        Timber.d("Processing of smsId $smsId")
                        if (chat.lastSmsId > smsId) {
                            Timber.d("Skipping lastSmsId is ${chat.lastSmsId}")
                            continue
                        }
                        val address = cursor.getString(cursor.getColumnIndexOrThrow(Sms.ADDRESS))
                        val body = cursor.getString(cursor.getColumnIndexOrThrow(Sms.BODY))
                        val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Sms.DATE))
                        val date = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)
                            .withZoneSameInstant(ZoneId.systemDefault())
                        val message = SendMessage(chat.id, """
                            $address
                            ```$body```
                            ${date.format(formatter)}
                        """.trimIndent())
                        val response = bot.execute(message.parseMode(ParseMode.Markdown))
                        if (response.isOk) {
                            db.chatDao().update(chat.also {
                                it.lastSmsId = smsId + 1
                            })
                            Timber.d("Updating $chat")
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

        private const val PARAM_SMS_ID = "sms_id"

        private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss (Z)")

        fun launch(context: Context, lastSmsId: Long?) {
            val request = OneTimeWorkRequestBuilder<MainWorker>()
                .setInputData(
                    Data.Builder()
                        .putLong(PARAM_SMS_ID, lastSmsId ?: 0L)
                        .build()
                )
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