package com.example.jewels.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.jewels.data.local.dao.AMBranchDao
import com.example.jewels.data.local.entity.BranchEntity

@Database(
    entities = [BranchEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun branchDao(): AMBranchDao
}
