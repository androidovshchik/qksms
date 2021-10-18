package androidovshchik.tg.sms

import android.content.Context
import android.provider.Telephony.Sms
import org.jetbrains.anko.doAsync

object Custom {

    fun init(context: Context) {
        val preferences = Preferences(context)
        if (preferences.lastSmsId >= 0) {
            return
        }
        doAsync {
            context.contentResolver.query(Sms.Inbox.CONTENT_URI, null, null, null, "${Sms._ID} DESC")?.use {
                if (it.moveToFirst()) {
                    preferences.lastSmsId = it.getInt(it.getColumnIndexOrThrow(Sms._ID))
                }
            }
        }
    }
}