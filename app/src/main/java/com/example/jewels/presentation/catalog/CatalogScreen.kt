package com.example.jewels.presentation.catalog

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.room.withTransaction
import coil.compose.AsyncImage
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.entity.ProductStatus
import com.example.jewels.presentation.components.NaoluxHeader
import com.example.jewels.presentation.components.premium.GoldButton
import com.example.jewels.presentation.components.premium.PremiumCard
import com.example.jewels.presentation.premium.PremiumStatusChip
import com.example.jewels.presentation.reservations.AddInterestDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class CatalogFilter { ALL, AVAILABLE, SOLD_OUT }

@Composable
fun CatalogScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val productDao = remember { db.productDao() }
    val interestDao = remember { db.interestDao() }
    val scope = rememberCoroutineScope()

    var showInterestDialog by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }

    var filter by remember { mutableStateOf(CatalogFilter.ALL) }

    val products by when (filter) {
        CatalogFilter.ALL ->
            productDao.observeAllWithPhotos().collectAsState(initial = emptyList())

        CatalogFilter.AVAILABLE ->
            productDao.observeByStatusWithPhotos(ProductStatus.AVAILABLE).collectAsState(initial = emptyList())

        CatalogFilter.SOLD_OUT ->
            productDao.observeByStatusWithPhotos(ProductStatus.SOLD_OUT).collectAsState(initial = emptyList())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        NaoluxHeader("Catálogo")
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = filter == CatalogFilter.ALL,
                onClick = { filter = CatalogFilter.ALL },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) { Text("Todos") }

            SegmentedButton(
                selected = filter == CatalogFilter.AVAILABLE,
                onClick = { filter = CatalogFilter.AVAILABLE },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) { Text("Disponibles") }

            SegmentedButton(
                selected = filter == CatalogFilter.SOLD_OUT,
                onClick = { filter = CatalogFilter.SOLD_OUT },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) { Text("Agotados") }
        }

        Spacer(Modifier.height(12.dp))

        if (products.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("No hay productos para este filtro.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Prueba otro filtro o agrega productos desde Inventario.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(products, key = { it.product.id }) { item ->
                    val p = item.product
                    val photoUris = item.photos
                        .map { it.uri }
                        .filter { it.isNotBlank() }     // ✅ evita strings vacíos
                    val thumb = photoUris.firstOrNull()

                    val canReserve = (p.status == ProductStatus.AVAILABLE && p.stock > 0)

                    CatalogProductCard(
                        name = p.name,
                        description = p.description,
                        priceClp = p.priceClp,
                        stock = p.stock,
                        status = p.status,
                        photoUris = photoUris,
                        thumbUri = thumb,
                        canReserve = canReserve,
                        onReserve = {
                            selectedProductId = p.id
                            showInterestDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showInterestDialog && selectedProductId != null) {
        val productId = selectedProductId!!

        AddInterestDialog(
            onDismiss = { showInterestDialog = false },
            onSave = { name, phone, note ->
                scope.launch(Dispatchers.IO) {
                    db.withTransaction {
                        val updatedRows = productDao.decrementStockIfAvailable(productId)

                        if (updatedRows == 1) {
                            interestDao.insert(
                                InterestEntity(
                                    productId = productId,
                                    buyerName = name,
                                    phone = phone,
                                    note = note,
                                    status = InterestStatus.PENDING
                                )
                            )
                            productDao.markSoldOutIfNoStock(productId)
                        } else {
                            scope.launch(Dispatchers.Main) {
                                Toast.makeText(context, "Sin stock disponible", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                showInterestDialog = false
            }
        )
    }
}

@Composable
private fun CatalogProductCard(
    name: String,
    description: String,
    priceClp: Int,
    stock: Int,
    status: ProductStatus,
    photoUris: List<String>,
    thumbUri: String?,
    canReserve: Boolean,
    onReserve: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    var showGallery by remember { mutableStateOf(false) }

    val emphasized = (status == ProductStatus.AVAILABLE && stock > 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) // ✅ dorado sutil
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f))
                        .clickable(enabled = photoUris.isNotEmpty()) { showGallery = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbUri.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbUri, // ✅ SIN Uri.parse()
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        PremiumStatusChip(
                            text = if (emphasized) "Disponible" else "Agotado",
                            emphasized = emphasized
                        )
                    }

                    if (description.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$$priceClp CLP",
                            style = MaterialTheme.typography.titleLarge
                        )
                        GoldButton(
                            text = if (canReserve) "Reservar" else "No disponible",
                            onClick = onReserve,
                            enabled = canReserve
                        )
                    }
                }
            }

            // Precio + meta

            Text(
                text = "Stock: $stock",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

        }
    }

    // Galería
    if (showGallery) {
        Dialog(onDismissRequest = { showGallery = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Fotos: ${photoUris.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(photoUris, key = { it }) { uriStr ->
                            AsyncImage(
                                model = uriStr, // ✅ SIN Uri.parse()
                                contentDescription = null,
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showGallery = false }) { Text("Cerrar") }
                    }
                }
            }
        }
    }
}
