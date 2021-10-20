package androidovshchik.tg.sms.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Chat::class
    ],
    version = 1
)
internal abstract class Database : RoomDatabase() {

    abstract fun chatDao(): ChatDao
}