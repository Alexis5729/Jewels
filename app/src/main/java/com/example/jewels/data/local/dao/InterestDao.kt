package com.example.jewels.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.model.InterestWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface InterestDao {

    @Insert
    suspend fun insert(interest: InterestEntity): Long

    @Update
    suspend fun update(interest: InterestEntity)

    @Query("SELECT * FROM interests ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InterestEntity>>

    @Query("SELECT * FROM interests WHERE status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: InterestStatus): Flow<List<InterestEntity>>

    @Query("SELECT * FROM interests WHERE productId = :productId ORDER BY createdAt DESC")
    fun observeByProduct(productId: Long): Flow<List<InterestEntity>>


    @Query("""
    SELECT i.id, i.productId, p.name as productName, i.buyerName, i.phone, i.note, i.status, i.createdAt
    FROM interests i
    INNER JOIN products p ON p.id = i.productId
    ORDER BY i.createdAt DESC
""")
    fun observeAllWithProduct(): Flow<List<InterestWithProduct>>

    @Query("""
    SELECT i.id, i.productId, p.name as productName, i.buyerName, i.phone, i.note, i.status, i.createdAt
    FROM interests i
    INNER JOIN products p ON p.id = i.productId
    WHERE i.status = :status
    ORDER BY i.createdAt DESC
""")
    fun observeByStatusWithProduct(status: InterestStatus): Flow<List<InterestWithProduct>>

}
