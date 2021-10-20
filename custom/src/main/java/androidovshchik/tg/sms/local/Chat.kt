package androidovshchik.tg.sms.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats"
)
internal class Chat(
    @PrimaryKey
    @ColumnInfo(name = "c_id")
    var id: Long = 0L,
    @ColumnInfo(name = "c_last_sms_id")
    var lastSmsId: Long = 0L
) {

    override fun toString(): String {
        return "Chat(id=$id, lastSmsId=$lastSmsId)"
    }
}