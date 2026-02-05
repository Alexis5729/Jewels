package com.example.jewels.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.jewels.data.local.dao.AMBranchDao
import com.example.jewels.data.local.dao.ProductDao
import com.example.jewels.data.local.dao.ProductPhotoDao
import com.example.jewels.data.local.entity.BranchEntity
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductPhotoEntity
import com.example.jewels.data.local.dao.InterestDao
import com.example.jewels.data.local.entity.InterestEntity

@Database(
    entities = [
        BranchEntity::class,
        ProductEntity::class,
        ProductPhotoEntity::class,
        InterestEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun branchDao(): AMBranchDao
    abstract fun productDao(): ProductDao
    abstract fun photoDao(): ProductPhotoDao
    abstract fun interestDao(): InterestDao
}