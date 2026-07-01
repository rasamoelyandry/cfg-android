package com.cfg.android.data.repository

import com.cfg.android.data.local.dao.MenuDao
import com.cfg.android.data.local.entity.MenuItemEntity
import com.cfg.android.data.remote.ApiService
import com.cfg.android.data.remote.dto.CategoryDto
import com.cfg.android.data.remote.dto.MenuItemDto
import com.cfg.android.data.remote.dto.MenuResponse
import com.cfg.android.util.NetworkMonitor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MenuRepository @Inject constructor(
    private val apiService: ApiService,
    private val menuDao: MenuDao,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun getMenu(restaurantId: String): Result<MenuResponse> =
        if (networkMonitor.isOnline.value) fetchAndCache(restaurantId)
        else getFromCache(restaurantId)

    private suspend fun fetchAndCache(restaurantId: String): Result<MenuResponse> = try {
        val response = apiService.getMenu(restaurantId)
        if (response.isSuccessful && response.body()?.data != null) {
            val menu = response.body()!!.data!!
            val entities = menu.categories.flatMap { cat ->
                cat.items.map { it.toEntity(restaurantId, cat.name) }
            }
            menuDao.clearForRestaurant(restaurantId)
            menuDao.upsertAll(entities)
            Result.success(menu)
        } else {
            getFromCache(restaurantId)
        }
    } catch (e: Exception) {
        getFromCache(restaurantId)
    }

    private suspend fun getFromCache(restaurantId: String): Result<MenuResponse> {
        val items = menuDao.getMenuItems(restaurantId)
        return if (items.isNotEmpty()) {
            val categories = items
                .groupBy { it.categoryId to it.categoryName }
                .map { (key, catItems) ->
                    CategoryDto(id = key.first, name = key.second, items = catItems.map { it.toDto() })
                }
            Result.success(MenuResponse(categories))
        } else {
            Result.failure(Exception("Aucune donnée menu disponible hors ligne"))
        }
    }
}

private fun MenuItemDto.toEntity(restaurantId: String, categoryName: String) = MenuItemEntity(
    id = id,
    restaurantId = restaurantId,
    categoryId = categoryId,
    categoryName = categoryName,
    name = name,
    description = description,
    price = price,
    imageUrl = imageUrl,
    isAvailable = isAvailable,
    sortOrder = sortOrder
)

private fun MenuItemEntity.toDto() = MenuItemDto(
    id = id,
    categoryId = categoryId,
    name = name,
    description = description,
    price = price,
    imageUrl = imageUrl,
    isAvailable = isAvailable,
    sortOrder = sortOrder,
    modifiers = emptyList()
)
