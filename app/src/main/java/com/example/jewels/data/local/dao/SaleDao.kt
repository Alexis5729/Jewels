package com.example.jewels.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.jewels.data.local.entity.SaleEntity
import com.example.jewels.data.local.model.SaleWithProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: SaleEntity)

    @Query("""
        SELECT 
            s.id as id,
            s.productId as productId,
            p.name as productName,
            s.buyerName as buyerName,
            s.phone as phone,
            s.note as note,
            s.priceClp as priceClp,
            s.createdAt as createdAt
        FROM sales s
        INNER JOIN products p ON p.id = s.productId
        ORDER BY s.createdAt DESC
    """)
    fun observeAllWithProduct(): Flow<List<SaleWithProduct>>
}
