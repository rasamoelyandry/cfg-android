package com.cfg.android.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cfg.android.data.local.dao.MenuDao
import com.cfg.android.data.local.dao.OrderDao
import com.cfg.android.data.local.dao.SyncQueueDao
import com.cfg.android.data.local.entity.MenuItemEntity
import com.cfg.android.data.local.entity.OrderEntity
import com.cfg.android.data.local.entity.SyncQueueEntity

@Database(
    entities = [OrderEntity::class, SyncQueueEntity::class, MenuItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun orderDao(): OrderDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun menuDao(): MenuDao
}
