package com.photodashboard.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.photodashboard.app.upload.UploadState
import kotlinx.coroutines.flow.Flow

/** A photo taken on this device (local file) or listed from the server (remote only). */
@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey val localId: String,
    val userId: Long,
    val filePath: String?,
    val mediaStoreUri: String?,
    val capturedAt: Long,
    val utcOffsetMinutes: Int,
    val latitude: Double?,
    val longitude: Double?,
    val altitudeM: Double?,
    val accuracyM: Float?,
    val bearingDeg: Float?,
    val speedMps: Float?,
    val locationProvider: String?,
    val noLocation: Boolean,
    val width: Int?,
    val height: Int?,
    val sizeBytes: Long?,
    val state: UploadState,
    /** Server photo_id, kept across retries so uploads stay idempotent (AND-15). */
    val serverPhotoId: String? = null,
    val s3Key: String? = null,
    val publicUrl: String? = null,
    val attempts: Int = 0,
    val lastError: String? = null,
)

class Converters {
    @TypeConverter
    fun fromState(s: UploadState): String = s.name

    @TypeConverter
    fun toState(s: String): UploadState = UploadState.valueOf(s)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE userId = :userId ORDER BY capturedAt DESC")
    fun observe(userId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE localId = :id")
    suspend fun get(id: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE localId = :id")
    fun observeOne(id: String): Flow<PhotoEntity?>

    @Query("SELECT * FROM photos WHERE serverPhotoId = :serverId LIMIT 1")
    suspend fun byServerId(serverId: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE userId = :userId AND state IN ('PENDING', 'UPLOADING', 'FAILED')")
    suspend fun unfinished(userId: Long): List<PhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(photo: PhotoEntity)

    @Update
    suspend fun update(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE localId = :id")
    suspend fun delete(id: String)
}

@Database(entities = [PhotoEntity::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class PhotoDb : RoomDatabase() {
    abstract fun photos(): PhotoDao

    companion object {
        fun create(context: Context): PhotoDb =
            Room.databaseBuilder(context, PhotoDb::class.java, "photos.db").build()
    }
}
