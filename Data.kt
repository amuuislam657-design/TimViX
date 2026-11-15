package com.videotube.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val category: String,
    val videoPath: String,
    val thumbPath: String?,
    val durationMs: Long,
    val createdAt: Long,
    val isShort: Boolean
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val username: String = "Creator",
    val bio: String = "",
    val avatarPath: String? = null
)

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos ORDER BY createdAt DESC") fun all(): Flow<List<VideoEntity>>
    @Insert suspend fun insert(v: VideoEntity): Long
    @Update suspend fun update(v: VideoEntity)
    @Delete suspend fun delete(v: VideoEntity)
    @Query("SELECT * FROM profile WHERE id = 1") fun profile(): Flow<ProfileEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProfile(p: ProfileEntity)
}

@Database(entities = [VideoEntity::class, ProfileEntity::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): VideoDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(c: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, AppDb::class.java, "videotube.db")
                .build().also { inst = it }
        }
    }
}
