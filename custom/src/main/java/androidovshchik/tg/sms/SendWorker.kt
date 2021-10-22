package androidovshchik.tg.sms

import android.content.Context
import androidovshchik.tg.sms.local.Preferences
import androidx.work.*
import androidx.work.WorkInfo.State
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SendWorker(appContext: Context, workerParams: WorkerParameters): Worker(appContext, workerParams) {

    override fun doWork(): Result = with(applicationContext) {
        val preferences = Preferences(applicationContext)
        val token = preferences.botToken?.trim()
        if (token.isNullOrBlank()) {
            Timber.w("Bot token is not set")
            return@with Result.failure()
        }
        val chats = db.chatDao().selectAll()
        Timber.d(chats.toString())
        if (chats.isEmpty()) {
            Timber.w("There are no chats")
            return@with Result.failure()
        }
        var hasErrors = false
        val lastMessages = db.messageDao().selectFromId(chats.first().nextMsgId)
        Timber.d(lastMessages.toString())
        val bot = TelegramBot(token)
        for (chat in chats) {
            for (message in lastMessages) {
                try {
                    Timber.d("Processing of smsId $smsId")
                    if (chat.nextMsgId > smsId) {
                        Timber.d("Skipping nextSmsId is ${chat.nextMsgId}")
                        continue
                    }
                    val sendMsg = SendMessage(chat.id, """
                        ${message.address}
                        ```${message.text}```
                        ${message.datetime.format(formatter)}
                    """.trimIndent())
                    val response = bot.execute(sendMsg.parseMode(ParseMode.Markdown))
                    if (response.isOk) {
                        db.chatDao().update(chat.also {
                            it.nextMsgId = message.id + 1
                        })
                        Timber.d("Updating $chat")
                    }
                } catch (e: Throwable) {
                    Timber.e(e)
                    hasErrors = true
                    break
                }
            }
        }
        return if (hasErrors) Result.retry() else Result.success()
    }

    companion object {

        private const val NAME = "Send"

        private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss [Z]")

        private val lock = Any()

        fun launch(context: Context) = synchronized(lock) {
            val request = OneTimeWorkRequestBuilder<SendWorker>()
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.SECONDS)
                .build()
            with(WorkManager.getInstance(context)) {
                val workInfos = getWorkInfosForUniqueWork(NAME).get()
                val count = workInfos.filter { it.state == State.ENQUEUED || it.state == State.RUNNING }
                    .size
                if (count < 2) {
                    enqueueUniqueWork(NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
                }
            }
        }

        fun cancel(context: Context) {
            with(WorkManager.getInstance(context)) {
                cancelUniqueWork(NAME)
            }
        }
    }
}