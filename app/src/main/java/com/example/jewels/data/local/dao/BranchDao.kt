package com.example.jewels.data.local.dao

import androidx.room.*
import com.example.jewels.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AMBranchDao {

    @Insert
    suspend fun insert(branch: BranchEntity): Long

    @Update
    suspend fun update(branch: BranchEntity)

    @Delete
    suspend fun delete(branch: BranchEntity)

    @Query("SELECT * FROM branches WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<BranchEntity>>

    @Query("SELECT * FROM branches WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BranchEntity?
}
