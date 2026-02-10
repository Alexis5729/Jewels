package com.example.jewels.data.local.dao

import androidx.room.*
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductStatus
import kotlinx.coroutines.flow.Flow
import androidx.room.Transaction
import com.example.jewels.data.local.model.ProductWithPhotos

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

    @Transaction
    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    fun observeAllWithPhotos(): Flow<List<ProductWithPhotos>>

    @Transaction
    @Query("SELECT * FROM products WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatusWithPhotos(status: com.example.jewels.data.local.entity.ProductStatus): Flow<List<ProductWithPhotos>>

    @Query("""
    UPDATE products
    SET stock = stock - 1
    WHERE id = :productId AND stock > 0 """)
    suspend fun decrementStockIfAvailable(productId: Long): Int
    // retorna filas afectadas (1 si pudo, 0 si no había stock)

    @Query("""
    UPDATE products
    SET status = :soldOut
    WHERE id = :productId AND stock <= 0 """)
    suspend fun markSoldOutIfNoStock(productId: Long, soldOut: ProductStatus = ProductStatus.SOLD_OUT)

    @Query("UPDATE products SET stock = stock + 1, updatedAt = :now WHERE id = :productId")
    suspend fun incrementStock(productId: Long, now: Long = System.currentTimeMillis())

    @Query("""
    UPDATE products
    SET status = :available, updatedAt = :now
    WHERE id = :productId AND stock > 0
""")
    suspend fun markAvailableIfHasStock(
        productId: Long,
        available: ProductStatus = ProductStatus.AVAILABLE,
        now: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): com.example.jewels.data.local.entity.ProductEntity?

}
