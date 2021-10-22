package androidovshchik.tg.sms.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Chat::class,
        Message::class
    ],
    version = 3
)
@TypeConverters(Converters::class)
internal abstract class Database : RoomDatabase() {

    abstract fun chatDao(): ChatDao

    abstract fun messageDao(): MessageDao
}