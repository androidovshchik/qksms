package androidovshchik.tg.sms

import android.content.Context
import androidovshchik.tg.sms.local.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jakewharton.threetenabp.AndroidThreeTen

internal lateinit var db: Database

object Custom {

    fun init(context: Context) {
        db = Room.databaseBuilder(context, Database::class.java, "custom.db")
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
        AndroidThreeTen.init(context)
    }
}