package com.example.jewels.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductPhotoEntity

data class ProductWithPhotos(
    @Embedded val product: ProductEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val photos: List<ProductPhotoEntity>
)
