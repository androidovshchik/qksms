package androidovshchik.tg.sms.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal abstract class MessageDao {

    @Query(
        """
        SELECT * FROM messages
        WHERE m_id >= :id
        ORDER BY m_id ASC
    """
    )
    abstract fun selectFromId(id: Long): List<Message>

    @Query(
        """
        SELECT m_id FROM messages
        ORDER BY m_id DESC
        LIMIT 1
    """
    )
    abstract fun getLastId(): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract fun insert(item: Message)
}