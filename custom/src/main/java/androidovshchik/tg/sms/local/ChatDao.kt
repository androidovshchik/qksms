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
    abstract fun selectAll(): List<Chat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(item: Chat): Long

    @Query(
        """
        DELETE FROM chats
    """
    )
    abstract fun deleteAll()
}