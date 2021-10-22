package androidovshchik.tg.sms.local

import androidx.room.TypeConverter
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime

@Suppress("unused")
object Converters {

    @JvmStatic
    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime?): Long? {
        return value?.withZoneSameInstant(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli()
    }

    @JvmStatic
    @TypeConverter
    fun toZonedDateTime(value: Long?): ZonedDateTime? {
        return value?.let {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault())
        }
    }
}