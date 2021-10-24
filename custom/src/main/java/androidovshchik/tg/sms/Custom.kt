package androidovshchik.tg.sms

import android.content.ContentValues
import android.content.Context
import android.provider.Telephony.Sms
import androidovshchik.tg.sms.local.Database
import androidovshchik.tg.sms.local.Message
import androidovshchik.tg.sms.local.Preferences
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.Instant
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

internal lateinit var db: Database

object Custom {

    fun init(context: Context) {
        db = Room.databaseBuilder(context, Database::class.java, "custom.db")
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        AndroidThreeTen.init(context)
        val preferences = Preferences(context)
        Timber.plant(LogTree(preferences.saveLogs))
    }

    fun saveSms(values: ContentValues) {
        val timestamp = values.getAsLong(Sms.DATE_SENT)
        db.messageDao().insert(Message(
            text = values.getAsString(Sms.BODY),
            address = values.getAsString(Sms.ADDRESS),
            datetime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC)
        ))
    }
}