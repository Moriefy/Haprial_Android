package com.haprial.app.data.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: Int,
    val title: String, val content: String, val tags: String,
    val category: String, val excerpt: String, val date: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface ArticleDao {
    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getDraft(id: Int): DraftEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DraftEntity)
    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteDraft(id: Int)
}

@Database(entities = [DraftEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao
    companion object {
        fun create(context: Context): AppDatabase = Room.databaseBuilder(
            context.applicationContext, AppDatabase::class.java, "haprial.db"
        ).build()
    }
}
