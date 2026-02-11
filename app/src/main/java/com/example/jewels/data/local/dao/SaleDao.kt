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

    // Para filtros SIN join (si lo necesitas)
    @Query("SELECT * FROM sales ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SaleEntity>>

    @Query("DELETE FROM sales WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("""
        SELECT * FROM sales
        WHERE createdAt BETWEEN :from AND :to
        ORDER BY createdAt DESC
    """)
    fun observeBetween(from: Long, to: Long): Flow<List<SaleEntity>>

    // ✅ Para mostrar Ventas con nombre de producto (lo que estás usando en pantalla)
    @Query("""
    SELECT 
        s.id as id,
        s.productId as productId,
        s.interestId as interestId,
        p.name as productName,
        s.buyerName as buyerName,
        s.phone as phone,
        s.note as note,
        s.priceClp as priceClp,
        s.qty as qty,
        s.createdAt as createdAt
    FROM sales s
    INNER JOIN products p ON p.id = s.productId
    ORDER BY s.createdAt DESC
""")
    fun observeAllWithProduct(): Flow<List<SaleWithProduct>>

    // ✅ Filtro con join (este es el que te conviene para la pantalla)
    @Query("""
        SELECT 
            s.id as id,
            s.productId as productId,
            s.interestId as interestId,
            p.name as productName,
            s.buyerName as buyerName,
            s.phone as phone,
            s.note as note,
            s.priceClp as priceClp,
            s.qty as qty,
            s.createdAt as createdAt
        FROM sales s
        INNER JOIN products p ON p.id = s.productId
        WHERE s.createdAt BETWEEN :from AND :to
        ORDER BY s.createdAt DESC
    """)
    fun observeBetweenWithProduct(from: Long, to: Long): Flow<List<SaleWithProduct>>
}
