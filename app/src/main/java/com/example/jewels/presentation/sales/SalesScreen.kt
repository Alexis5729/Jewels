package com.example.jewels.presentation.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.model.SaleWithProduct
import com.example.jewels.presentation.components.NaoluxHeader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class SalesFilter { ALL, TODAY, LAST_7, LAST_30 }

@Composable
fun SalesScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val saleDao = remember { db.saleDao() }
    val productDao = remember { db.productDao() }
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
        SalesFilter.ALL -> saleDao.observeAllWithProduct().collectAsState(initial = emptyList())
        SalesFilter.TODAY -> saleDao.observeBetweenWithProduct(fromToday, System.currentTimeMillis()).collectAsState(initial = emptyList())
        SalesFilter.LAST_7 -> saleDao.observeBetweenWithProduct(from7, System.currentTimeMillis()).collectAsState(initial = emptyList())
        SalesFilter.LAST_30 -> saleDao.observeBetweenWithProduct(from30, System.currentTimeMillis()).collectAsState(initial = emptyList())
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

        // Filtro
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

        // Resumen
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Total ventas: ${sales.size}")
                Text("Total: $${totalClp} CLP")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (sales.isEmpty()) {
            Text("Aún no hay ventas registradas para este filtro.")
        } else {
            LazyColumn {
                items(sales, key = { it.id }) { s ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(s.buyerName, style = MaterialTheme.typography.titleMedium)
                            Text("Producto: ${s.productName}")
                            Text("Tel: ${s.phone}")
                            if (s.note.isNotBlank()) Text("Nota: ${s.note}")
                            Spacer(Modifier.height(6.dp))
                            Text("Precio: $${s.priceClp} CLP")
                            Text("Fecha: ${formatDate(s.createdAt)}")

                            Spacer(Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = {
                                    deleteTarget = s
                                    confirmDeleteOpen = true
                                }
                            ) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }

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

                                        // 2) reabrir reserva (NO borrarla)
                                        s.interestId?.let { interestId ->
                                            interestDao.updateStatus(interestId, InterestStatus.CONTACTED)
                                            // o PENDING si prefieres
                                        }
                                    }
                                }
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
