package cl.inacap.iluminacioniot.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users", indices = [Index(value = ["username"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val salt: String,
    val hash: String
)

@Entity(tableName = "readings")
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val value: Int,
    val timestamp: Long
)

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun find(username: String): User?
}

@Dao
interface ReadingDao {
    @Insert
    suspend fun insert(reading: Reading)

    @Query("SELECT * FROM readings ORDER BY timestamp DESC LIMIT :n")
    fun latest(n: Int): Flow<List<Reading>>
}

@Database(entities = [User::class, Reading::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun readingDao(): ReadingDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "iluminacion.db"
            ).build().also { instance = it }
        }
    }
}
