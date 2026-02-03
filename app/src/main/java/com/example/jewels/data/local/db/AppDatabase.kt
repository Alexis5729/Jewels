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

@Database(
    entities = [
        BranchEntity::class,
        ProductEntity::class,
        ProductPhotoEntity::class
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun branchDao(): AMBranchDao
    abstract fun productDao(): ProductDao
    abstract fun photoDao(): ProductPhotoDao
}
