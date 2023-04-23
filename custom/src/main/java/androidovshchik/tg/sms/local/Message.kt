package androidovshchik.tg.sms.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

@Entity(
    tableName = "messages"
)
class Message(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "m_id")
    var id: Long = 0L,
    @ColumnInfo(name = "m_text")
    var text: String?,
    @ColumnInfo(name = "m_address")
    var address: String?,
    @ColumnInfo(name = "m_datetime")
    var datetime: ZonedDateTime
) {

    override fun toString(): String {
        return "Message(" +
            "id=$id, " +
            "text=$text, " +
            "address=$address, " +
            "datetime=${datetime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}" +
            ")"
    }
}