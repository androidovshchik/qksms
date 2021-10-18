package androidovshchik.tg.sms.local

import androidx.room.*

@Dao
internal abstract class ChatDao {

    @Query(
        """
        SELECT * FROM tokens
        ORDER BY t_id ASC
    """
    )
    abstract fun getAll(): List<Chat>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: Chat): Long

    @Delete
    abstract fun delete(items: List<Chat>)
}