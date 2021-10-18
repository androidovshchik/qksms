package androidovshchik.tg.sms.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal abstract class ChatDao {

    @Query(
        """
        SELECT * FROM chats
        ORDER BY c_last_sms_id ASC
    """
    )
    abstract fun selectAll(): MutableList<Chat>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(item: Chat): Long

    @Query(
        """
        UPDATE chats
        SET c_last_sms_id = :chatId
        WHERE c_id = :id
    """
    )
    abstract fun updateChat(id: Long, chatId: Int)

    @Query(
        """
        DELETE FROM chats
    """
    )
    abstract fun deleteAll()
}