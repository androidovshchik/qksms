package androidovshchik.tg.sms

import android.content.Context
import android.provider.Telephony.Sms
import androidovshchik.tg.sms.local.Database
import androidovshchik.tg.sms.local.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import org.jetbrains.anko.doAsync

internal lateinit var db: Database

object Custom {

    fun init(context: Context) {
        db = Room.databaseBuilder(context, Database::class.java, "custom.db")
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
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