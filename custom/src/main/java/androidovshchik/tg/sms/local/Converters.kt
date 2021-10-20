package androidovshchik.tg.sms.local

import androidx.room.TypeConverter
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

@Suppress("unused")
object Converters {

    @JvmStatic
    @TypeConverter
    fun fromZonedDateTime(value: ZonedDateTime?): String? {
        return value?.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    @JvmStatic
    @TypeConverter
    fun toZonedDateTime(value: String?): ZonedDateTime? {
        return value?.let { ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
    }
}