package com.example.jewels.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.jewels.data.local.entity.ProductPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductPhotoDao {

    @Insert
    suspend fun insert(photo: ProductPhotoEntity): Long

    @Delete
    suspend fun delete(photo: ProductPhotoEntity)

    @Query("SELECT * FROM product_photos WHERE productId = :productId ORDER BY createdAt ASC")
    fun observeByProduct(productId: Long): Flow<List<ProductPhotoEntity>>
}
