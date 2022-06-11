package com.fivegen.aquariuslocation.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.util.*

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}

@Entity(tableName = "location")
data class DbLocation(
    @PrimaryKey val date: Date,
    @ColumnInfo val latitude: Double,
    @ColumnInfo val longitude: Double,
    @ColumnInfo val altitude: Double
)

@Dao
interface LocationDao {
    @Query("SELECT * FROM location")
    fun getAll(): List<DbLocation>

    @Query("SELECT * FROM location ORDER BY date")
    fun flowAll(): Flow<List<DbLocation>>

    @Insert
    fun insert(loc: DbLocation)

    @Query("DELETE FROM location")
    fun deleteAll()
}

@Database(entities = [DbLocation::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}
