package com.example.jewels.data.local.dao

import androidx.room.*
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Delete
    suspend fun delete(product: ProductEntity)

    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: ProductStatus): Flow<List<ProductEntity>>
}
