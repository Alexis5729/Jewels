package com.example.jewels.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.jewels.data.local.dao.AMBranchDao
import com.example.jewels.data.local.dao.ProductDao
import com.example.jewels.data.local.dao.ProductPhotoDao
import com.example.jewels.data.local.entity.BranchEntity
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductPhotoEntity
import com.example.jewels.data.local.dao.InterestDao
import com.example.jewels.data.local.dao.SaleDao
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.SaleEntity

@Database(
    entities = [
        BranchEntity::class,
        ProductEntity::class,
        ProductPhotoEntity::class,
        InterestEntity::class,
        SaleEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun branchDao(): AMBranchDao
    abstract fun productDao(): ProductDao
    abstract fun photoDao(): ProductPhotoDao
    abstract fun interestDao(): InterestDao
    abstract fun saleDao(): SaleDao
}