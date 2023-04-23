package androidovshchik.tg.sms

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidovshchik.tg.sms.local.Message
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.*

class NotificationService : NotificationListenerService() {

    override fun onNotificationPosted(notification: StatusBarNotification) {
        if (BuildConfig.DEBUG) {
            logNotification(notification)
        }
        if (notification.packageName == packageName) {
            return
        }
        if (notification.notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            return
        }
        Custom.saveSms(Message(
            text = """
                ${notification.notification.extras.getCharSequence(Notification.EXTRA_TITLE)}
                ${notification.notification.extras.getCharSequence(Notification.EXTRA_TEXT)}
            """.trimIndent(),
            address = notification.packageName,
            datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(notification.postTime), ZoneOffset.UTC)
        ))
        SendWorker.launch(applicationContext)
    }

    override fun onNotificationRemoved(notification: StatusBarNotification) {
    }

    private fun logNotification(notification: StatusBarNotification) {
        val char = chars[chars.indices.random()]
        div(char)
        log("New notification")
        log("id: ${notification.id}", char)
        log("packageName: ${notification.packageName}", char)
        notification.notification.actions?.let {
            log("Notification actions")
            it.forEachIndexed { i, action ->
                log("action.title[$i]: ${action.title}", char)
            }
        }
        notification.notification.extras?.let {
            log("Notification extras")
            map(it, char)
        }
        div(char)
    }

    companion object {

        private val tag = NotificationService::class.java.simpleName

        private val chars = arrayOf("*", ":", ";", "$", "#", "@", "&", "=", "\\", "/")

        private const val STYLED_LOG_LENGTH = 48

        private fun log(text: String, char: String = " ") {
            val length = text.length + 2
            if (length >= STYLED_LOG_LENGTH) {
                print("$char%s${text.substring(0, STYLED_LOG_LENGTH - 5)}%s...$char")
            } else {
                val result = "$char%s$text%s${if (length % 2 == 0) "" else " "}$char"
                print(result, repeat(" ", (STYLED_LOG_LENGTH - length) / 2))
            }
        }

        private fun map(extras: Bundle, char: String) {
            for (key in extras.keySet()) {
                log("$key: ${extras[key]}", char)
            }
        }

        private fun div(char: String) {
            Timber.tag(tag).i(repeat(char, STYLED_LOG_LENGTH))
        }

        private fun print(text: String, edge: String = "") {
            Timber.tag(tag).i(text, edge, edge)
        }

        private fun repeat(what: String, times: Int): String {
            return Collections.nCopies(times, what).joinToString("")
        }
    }
}