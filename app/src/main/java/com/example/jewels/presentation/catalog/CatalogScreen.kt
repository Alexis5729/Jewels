package com.example.jewels.presentation.catalog

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.entity.ProductStatus
import com.example.jewels.presentation.reservations.AddInterestDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class CatalogFilter { ALL, AVAILABLE, SOLD_OUT }

@Composable
fun CatalogScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }   // ✅ misma instancia en toda la app
    val dao = remember { db.productDao() }

    val interestDao = remember { db.interestDao() }  // o DbProvider.get(context).interestDao()
    val scope = rememberCoroutineScope()

    var showInterestDialog by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var selectedProductName by remember { mutableStateOf("") }

    var filter by remember { mutableStateOf(CatalogFilter.ALL) }

    val products by when (filter) {
        CatalogFilter.ALL ->
            dao.observeAllWithPhotos().collectAsState(initial = emptyList())

        CatalogFilter.AVAILABLE ->
            dao.observeByStatusWithPhotos(ProductStatus.AVAILABLE).collectAsState(initial = emptyList())

        CatalogFilter.SOLD_OUT ->
            dao.observeByStatusWithPhotos(ProductStatus.SOLD_OUT).collectAsState(initial = emptyList())
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Catálogo", style = MaterialTheme.typography.titleLarge)
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
            Text("No hay productos para este filtro.")
        } else {
            products.forEach { item ->
                val p = item.product
                val photos = item.photos
                val thumb = photos.firstOrNull()?.uri

                ElevatedCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // Miniatura a la izquierda (si hay)
                        if (thumb != null) {
                            AsyncImage(
                                model = Uri.parse(thumb),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                        }

                        Column(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                AssistChip(
                                    onClick = {},
                                    label = { Text(if (p.status == ProductStatus.AVAILABLE) "Disponible" else "Agotado") }
                                )
                            }

                            if (p.description.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(p.description)
                            }

                            Spacer(Modifier.height(6.dp))
                            Text("Precio: $${p.priceClp} CLP")
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        selectedProductId = p.id
                        selectedProductName = p.name
                        showInterestDialog = true
                    },
                    enabled = (p.status == ProductStatus.AVAILABLE)
                ) {
                    Text(if (p.status == ProductStatus.AVAILABLE) "Reservar" else "No disponible")
                }

            }
        }
    }
    if (showInterestDialog && selectedProductId != null) {
        AddInterestDialog(
            onDismiss = { showInterestDialog = false },
            onSave = { name, phone, note ->
                scope.launch(Dispatchers.IO) {
                    interestDao.insert(
                        InterestEntity(
                            productId = selectedProductId!!,
                            buyerName = name,
                            phone = phone,
                            note = note,
                            status = InterestStatus.PENDING
                        )
                    )
                }
                showInterestDialog = false
            }
        )
    }

}