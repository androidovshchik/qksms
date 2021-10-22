package androidovshchik.tg.sms.local

import androidx.room.*

@Dao
internal abstract class ChatDao {

    @Query(
        """
        SELECT * FROM chats
        ORDER BY c_next_msg_id ASC
    """
    )
    abstract fun selectAll(): MutableList<Chat>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(item: Chat)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    abstract fun update(item: Chat)

    @Query(
        """
        DELETE FROM chats
    """
    )
    abstract fun deleteAll()
}