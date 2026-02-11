package com.example.jewels.presentation.inventory

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductPhotoEntity
import com.example.jewels.data.local.entity.ProductStatus
import com.example.jewels.presentation.components.NaoluxHeader
import com.example.jewels.presentation.components.premium.GoldButton
import com.example.jewels.presentation.components.premium.GoldOutlineButton
import com.example.jewels.presentation.components.premium.PremiumCard
import com.example.jewels.presentation.components.premium.PremiumStatusChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun InventoryScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val dao = remember { db.productDao() }
    val scope = rememberCoroutineScope()

    val products by dao.observeAllWithPhotos().collectAsState(initial = emptyList())
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            NaoluxHeader("Inventario")
            Spacer(Modifier.height(12.dp))

            if (products.isEmpty()) {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Aún no hay productos.",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Toca + para agregar tu primer producto.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(products, key = { it.product.id }) { item ->
                        val p = item.product
                        val photos = item.photos
                        val thumb = photos.firstOrNull()?.uri

                        PremiumCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedProduct = p }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (thumb != null) {
                                    AsyncImage(
                                        model = Uri.parse(thumb),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                Column(Modifier.weight(1f)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            p.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        PremiumStatusChip(
                                            if (p.status == ProductStatus.AVAILABLE) "Disponible" else "Agotado"
                                        )
                                    }

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        "Precio: $${p.priceClp} CLP",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "Stock: ${p.stock} • Fotos: ${photos.size}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Agregar producto")
        }

        if (showAdd) {
            AddProductDialog(
                onDismiss = { showAdd = false },
                onSave = { product ->
                    scope.launch(Dispatchers.IO) { dao.insert(product) }
                    showAdd = false
                }
            )
        }

        selectedProduct?.let { selected ->
            EditProductDialog(
                product = selected,
                onDismiss = { selectedProduct = null },
                onSave = { updated ->
                    selectedProduct = null
                    scope.launch(Dispatchers.IO) {
                        dao.update(updated.copy(updatedAt = System.currentTimeMillis()))
                    }
                },
                onDelete = { prod ->
                    scope.launch(Dispatchers.IO) { dao.delete(prod) }
                    selectedProduct = null
                }
            )
        }
    }
}

@Composable
private fun AddProductDialog(
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") }
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Precio (CLP)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock") },
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = available, onCheckedChange = { available = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (available) "Disponible" else "Agotado")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = price.toIntOrNull()
                    val s = stock.toIntOrNull()
                    if (name.isNotBlank() && p != null && s != null) {
                        onSave(
                            ProductEntity(
                                name = name.trim(),
                                description = desc.trim(),
                                priceClp = p,
                                stock = s,
                                status = if (available) ProductStatus.AVAILABLE else ProductStatus.SOLD_OUT
                            )
                        )
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun EditProductDialog(
    product: ProductEntity,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit,
    onDelete: (ProductEntity) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var desc by remember { mutableStateOf(product.description) }
    var price by remember { mutableStateOf(product.priceClp.toString()) }
    var stock by remember { mutableStateOf(product.stock.toString()) }
    var available by remember { mutableStateOf(product.status == ProductStatus.AVAILABLE) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val db = remember { DbProvider.get(context) }
    val photoDao = remember { db.photoDao() }
    val photos by photoDao.observeByProduct(product.id).collectAsState(initial = emptyList())

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            scope.launch(Dispatchers.IO) {
                photoDao.insert(
                    ProductPhotoEntity(
                        productId = product.id,
                        uri = uri.toString()
                    )
                )
            }
        }
    }

    var showInvalidMsg by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {

            // HEADER (fijo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Editar producto", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Actualiza datos y fotos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            // BODY (scroll)
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showInvalidMsg = false },
                    label = { Text("Nombre") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") }
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() }; showInvalidMsg = false },
                    label = { Text("Precio (CLP)") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() }; showInvalidMsg = false },
                    label = { Text("Stock") },
                    singleLine = true
                )

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Fotos (${photos.size})", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        photos.take(3).forEach { ph ->
                            AsyncImage(
                                model = Uri.parse(ph.uri),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    GoldOutlineButton(
                        text = "Agregar foto desde galería",
                        onClick = { pickImageLauncher.launch(arrayOf("image/*")) }
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = available, onCheckedChange = { available = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (available) "Disponible" else "Agotado")
                }

                if (showInvalidMsg) {
                    Text(
                        "Revisa: nombre, precio y stock deben ser válidos.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // FOOTER (fijo, siempre visible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onDelete(product) }) { Text("Eliminar", maxLines = 1) }

                Spacer(Modifier.weight(1f))

                TextButton(onClick = onDismiss) { Text("Cancelar", maxLines = 1) }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        val p = price.toIntOrNull()
                        val s = stock.toIntOrNull()
                        val ok = name.isNotBlank() && p != null && s != null
                        if (!ok) {
                            showInvalidMsg = true
                            return@Button
                        }

                        onSave(
                            product.copy(
                                name = name.trim(),
                                description = desc.trim(),
                                priceClp = p!!,
                                stock = s!!,
                                status = if (available) ProductStatus.AVAILABLE else ProductStatus.SOLD_OUT,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    },
                    modifier = Modifier.widthIn(min = 120.dp),
                ) { Text("Guardar", maxLines = 1) }
            }
        }
    }
}
