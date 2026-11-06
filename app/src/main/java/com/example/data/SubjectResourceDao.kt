package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectResourceDao {
    @Query("SELECT * FROM subject_resources ORDER BY id DESC")
    fun getAllResources(): Flow<List<SubjectResourceEntity>>

    @Query("SELECT * FROM subject_resources WHERE course = :course AND (:semester = 0 OR semester = :semester) ORDER BY id DESC")
    fun getResourcesByCourseAndSemester(course: String, semester: Int): Flow<List<SubjectResourceEntity>>

    @Query("""
        SELECT * FROM subject_resources 
        WHERE title LIKE '%' || :query || '%' 
           OR subjectName LIKE '%' || :query || '%' 
           OR subjectCode LIKE '%' || :query || '%'
           OR unitOrTopic LIKE '%' || :query || '%'
        ORDER BY id DESC
    """)
    fun searchResources(query: String): Flow<List<SubjectResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resource: SubjectResourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(resources: List<SubjectResourceEntity>)

    @Update
    suspend fun update(resource: SubjectResourceEntity)

    @Delete
    suspend fun delete(resource: SubjectResourceEntity)

    @Query("DELETE FROM subject_resources WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE subject_resources SET downloadsCount = downloadsCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: Long)

    @Query("SELECT COUNT(*) FROM subject_resources")
    suspend fun getCount(): Int
}
