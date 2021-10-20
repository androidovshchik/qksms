package androidovshchik.tg.sms.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

@Entity(
    tableName = "chats"
)
internal class Chat(
    @PrimaryKey
    @ColumnInfo(name = "c_id")
    var id: Long = 0L,
    @ColumnInfo(name = "c_sms_time")
    var smsTime: ZonedDateTime = ZonedDateTime.now()
) {

    override fun toString(): String {
        return "Chat(" +
            "id=$id, " +
            "smsTime=${smsTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}" +
            ")"
    }
}