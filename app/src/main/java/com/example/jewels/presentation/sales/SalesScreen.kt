package com.example.jewels.presentation.sales

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
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.model.SaleWithProduct
import com.example.jewels.presentation.components.NaoluxHeader
import com.example.jewels.presentation.components.premium.GoldOutlineButton
import com.example.jewels.presentation.components.premium.PremiumCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class SalesFilter { ALL, TODAY, LAST_7, LAST_30 }

@Composable
fun SalesScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val saleDao = remember { db.saleDao() }
    val interestDao = remember { db.interestDao() }
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(SalesFilter.ALL) }

    var confirmDeleteOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<SaleWithProduct?>(null) }

    val now = remember { System.currentTimeMillis() }
    val fromToday = remember(now) { startOfDayMillis(now) }
    val from7 = remember(now) { now - 7L * 24 * 60 * 60 * 1000 }
    val from30 = remember(now) { now - 30L * 24 * 60 * 60 * 1000 }

    val sales by when (filter) {
        SalesFilter.ALL ->
            saleDao.observeAllWithProduct().collectAsState(initial = emptyList())

        SalesFilter.TODAY ->
            saleDao.observeBetweenWithProduct(fromToday, System.currentTimeMillis())
                .collectAsState(initial = emptyList())

        SalesFilter.LAST_7 ->
            saleDao.observeBetweenWithProduct(from7, System.currentTimeMillis())
                .collectAsState(initial = emptyList())

        SalesFilter.LAST_30 ->
            saleDao.observeBetweenWithProduct(from30, System.currentTimeMillis())
                .collectAsState(initial = emptyList())
    }

    val totalClp = remember(sales) { sales.sumOf { it.priceClp } }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        NaoluxHeader("Ventas")
        Spacer(Modifier.height(12.dp))

        // ---- Filtro ----
        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = filter == SalesFilter.ALL,
                onClick = { filter = SalesFilter.ALL },
                shape = SegmentedButtonDefaults.itemShape(0, 4)
            ) { Text("Todo") }

            SegmentedButton(
                selected = filter == SalesFilter.TODAY,
                onClick = { filter = SalesFilter.TODAY },
                shape = SegmentedButtonDefaults.itemShape(1, 4)
            ) { Text("Hoy") }

            SegmentedButton(
                selected = filter == SalesFilter.LAST_7,
                onClick = { filter = SalesFilter.LAST_7 },
                shape = SegmentedButtonDefaults.itemShape(2, 4)
            ) { Text("7d") }

            SegmentedButton(
                selected = filter == SalesFilter.LAST_30,
                onClick = { filter = SalesFilter.LAST_30 },
                shape = SegmentedButtonDefaults.itemShape(3, 4)
            ) { Text("30d") }
        }

        Spacer(Modifier.height(12.dp))

        // ---- Resumen premium ----
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total ventas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${sales.size}",
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Total (CLP)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$$totalClp",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (sales.isEmpty()) {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Aún no hay ventas registradas.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Cuando registres una venta desde Reservas, aparecerá aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sales, key = { it.id }) { s ->
                    SaleCard(
                        sale = s,
                        onDelete = {
                            deleteTarget = s
                            confirmDeleteOpen = true
                        }
                    )
                }
            }
        }
    }

    // ---- Confirmación eliminar ----
    if (confirmDeleteOpen && deleteTarget != null) {
        val s = deleteTarget!!

        AlertDialog(
            onDismissRequest = {
                confirmDeleteOpen = false
                deleteTarget = null
            },
            title = { Text("Eliminar venta") },
            text = {
                Text(
                    "¿Seguro que quieres eliminar esta venta?\n\n" +
                            "Cliente: ${s.buyerName}\n" +
                            "Producto: ${s.productName}\n" +
                            "Precio: $${s.priceClp} CLP\n\n" +
                            "Esto ajustará el total mostrado en Ventas."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            db.withTransaction {
                                // 1) borrar venta
                                saleDao.deleteById(s.id)

                                // 2) reabrir reserva (si existe)
                                s.interestId?.let { interestId ->
                                    interestDao.updateStatus(interestId, InterestStatus.CONTACTED)
                                }
                            }
                        }
                        confirmDeleteOpen = false
                        deleteTarget = null
                    }
                ) { Text("Sí, eliminar") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDeleteOpen = false
                        deleteTarget = null
                    }
                ) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SaleCard(
    sale: SaleWithProduct,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) // dorado sutil
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = sale.buyerName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Producto: ${sale.productName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "$${sale.priceClp} CLP",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = "Tel: ${sale.phone}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (sale.note.isNotBlank()) {
                Text(
                    text = sale.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "Fecha: ${formatDate(sale.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                GoldOutlineButton(
                    text = "Eliminar",
                    onClick = onDelete
                )
            }
        }
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("es", "CL"))
    return sdf.format(Date(ms))
}

private fun startOfDayMillis(now: Long): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
