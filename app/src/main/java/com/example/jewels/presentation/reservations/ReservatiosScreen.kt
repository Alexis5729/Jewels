package com.example.jewels.presentation.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.model.InterestWithProduct
import com.example.jewels.presentation.components.NaoluxHeader
import com.example.jewels.presentation.components.premium.GoldButton
import com.example.jewels.presentation.components.premium.GoldOutlineButton
import com.example.jewels.presentation.components.premium.PremiumCard
import com.example.jewels.presentation.premium.PremiumStatusChip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class InterestFilter { ALL, PENDING, CONTACTED, CLOSED }

@Composable
fun ReservationsScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val interestDao = remember { db.interestDao() }
    val productDao = remember { db.productDao() }
    val saleDao = remember { db.saleDao() }
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(InterestFilter.ALL) }

    var confirmCancelOpen by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<InterestWithProduct?>(null) }

    var showSaleDialog by remember { mutableStateOf(false) }
    var saleTarget by remember { mutableStateOf<InterestWithProduct?>(null) }

    val list by when (filter) {
        InterestFilter.ALL -> interestDao.observeAllWithProduct().collectAsState(initial = emptyList())
        InterestFilter.PENDING -> interestDao.observeByStatusWithProduct(InterestStatus.PENDING).collectAsState(initial = emptyList())
        InterestFilter.CONTACTED -> interestDao.observeByStatusWithProduct(InterestStatus.CONTACTED).collectAsState(initial = emptyList())
        InterestFilter.CLOSED -> interestDao.observeByStatusWithProduct(InterestStatus.CLOSED).collectAsState(initial = emptyList())
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        NaoluxHeader("Reservas")
        Spacer(Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = filter == InterestFilter.ALL,
                onClick = { filter = InterestFilter.ALL },
                shape = SegmentedButtonDefaults.itemShape(0, 4)
            ) { Text("Todos") }

            SegmentedButton(
                selected = filter == InterestFilter.PENDING,
                onClick = { filter = InterestFilter.PENDING },
                shape = SegmentedButtonDefaults.itemShape(1, 4)
            ) { Text("Pend.") }

            SegmentedButton(
                selected = filter == InterestFilter.CONTACTED,
                onClick = { filter = InterestFilter.CONTACTED },
                shape = SegmentedButtonDefaults.itemShape(2, 4)
            ) { Text("Cont.") }

            SegmentedButton(
                selected = filter == InterestFilter.CLOSED,
                onClick = { filter = InterestFilter.CLOSED },
                shape = SegmentedButtonDefaults.itemShape(3, 4)
            ) { Text("Cerr.") }
        }

        Spacer(Modifier.height(12.dp))

        if (list.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("No hay reservas para este filtro.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cuando reserves desde Catálogo, aparecerán aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(list, key = { it.id }) { row ->
                    ReservationCard(
                        row = row,
                        onToggleStatus = {
                            if (row.status == InterestStatus.CLOSED) return@ReservationCard

                            val next = when (row.status) {
                                InterestStatus.PENDING -> InterestStatus.CONTACTED
                                InterestStatus.CONTACTED -> InterestStatus.PENDING
                                InterestStatus.CLOSED -> InterestStatus.CLOSED
                            }

                            scope.launch(Dispatchers.IO) {
                                interestDao.update(
                                    InterestEntity(
                                        id = row.id,
                                        productId = row.productId,
                                        buyerName = row.buyerName,
                                        phone = row.phone,
                                        note = row.note,
                                        status = next,
                                        createdAt = row.createdAt
                                    )
                                )
                            }
                        },
                        onRegisterSale = {
                            saleTarget = row
                            showSaleDialog = true
                        },
                        onCancel = {
                            cancelTarget = row
                            confirmCancelOpen = true
                        }
                    )
                }
            }
        }
    }

    // ---- Confirmación anular ----
    if (confirmCancelOpen && cancelTarget != null) {
        val target = cancelTarget!!

        AlertDialog(
            onDismissRequest = {
                confirmCancelOpen = false
                cancelTarget = null
            },
            title = { Text("Anular reserva") },
            text = {
                Text(
                    "¿Seguro que quieres anular la reserva de ${target.buyerName} para “${target.productName}”? " +
                            "Se devolverá 1 unidad al stock."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.withTransaction {
                                interestDao.deleteById(target.id)
                                productDao.incrementStock(target.productId)
                                productDao.markAvailableIfHasStock(target.productId)
                            }
                        }
                        confirmCancelOpen = false
                        cancelTarget = null
                    }
                ) { Text("Sí, anular") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmCancelOpen = false
                        cancelTarget = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }

    // ---- Dialog registrar venta ----
    if (showSaleDialog && saleTarget != null) {
        val row = saleTarget!!

        var price by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showSaleDialog = false
                saleTarget = null
            },
            title = { Text("Registrar venta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cliente: ${row.buyerName}")
                    Text("Producto: ${row.productName}")

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() } },
                        label = { Text("Precio final (CLP)") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Nota (opcional)") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val priceInt = price.toIntOrNull() ?: return@TextButton

                        scope.launch(Dispatchers.IO) {
                            db.withTransaction {
                                saleDao.insert(
                                    com.example.jewels.data.local.entity.SaleEntity(
                                        productId = row.productId,
                                        interestId = row.id,
                                        buyerName = row.buyerName,
                                        phone = row.phone,
                                        priceClp = priceInt,
                                        note = note,
                                        qty = 1
                                    )
                                )
                                interestDao.update(
                                    InterestEntity(
                                        id = row.id,
                                        productId = row.productId,
                                        buyerName = row.buyerName,
                                        phone = row.phone,
                                        note = row.note,
                                        status = InterestStatus.CLOSED,
                                        createdAt = row.createdAt
                                    )
                                )
                            }
                        }

                        showSaleDialog = false
                        saleTarget = null
                    }
                ) { Text("Confirmar venta") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSaleDialog = false
                        saleTarget = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ReservationCard(
    row: InterestWithProduct,
    onToggleStatus: () -> Unit,
    onRegisterSale: () -> Unit,
    onCancel: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val isClosed = row.status == InterestStatus.CLOSED
    val emphasized = !isClosed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), // ✅ CAMBIO: menos padding vertical
            verticalArrangement = Arrangement.spacedBy(6.dp) // ✅ CAMBIO: menos espacio entre bloques
        ) {

            // Nombre + estado
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.buyerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Producto: ${row.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text( // ✅ CAMBIO: Tel lo dejamos pegado al header (no abajo suelto)
                        text = "Tel: ${row.phone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                PremiumStatusChip(
                    text = when (row.status) {
                        InterestStatus.PENDING -> "PENDIENTE"
                        InterestStatus.CONTACTED -> "CONTACTADO"
                        InterestStatus.CLOSED -> "CERRADO"
                    },
                    emphasized = emphasized,
                    enabled = !isClosed, // ✅ no permitir cambiar si está cerrado
                    onClick = {
                        if (!isClosed) {
                            onToggleStatus()
                        }
                    }
                )
            }

            if (row.note.isNotBlank()) {
                Text(
                    text = row.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp)) // ✅ CAMBIO: separador pequeño, no gigante

            // ✅ CAMBIO: acciones en 2 filas (esto baja MUCHO la altura y evita "espacio muerto")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoldButton(
                    text = "Registrar venta",
                    onClick = onRegisterSale,
                    enabled = !isClosed,
                    modifier = Modifier.weight(0.5f) // ✅ CAMBIO: cada botón ocupa mitad
                )
                GoldOutlineButton(
                    text = "Anular",
                    onClick = onCancel,
                    enabled = !isClosed,
                    modifier = Modifier.weight(0.5f)
                )
            }
        }
    }
}
