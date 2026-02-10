package com.example.jewels.presentation.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import com.example.jewels.data.local.model.InterestWithProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class InterestFilter { ALL, PENDING, CONTACTED, CLOSED }

@Composable
fun ReservationsScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val interestDao = remember { db.interestDao() }
    val scope = rememberCoroutineScope()

    val productDao = remember { db.productDao() }
    val saleDao = remember { db.saleDao() }

    var filter by remember { mutableStateOf(InterestFilter.ALL) }

    var confirmCancelOpen by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<InterestWithProduct?>(null) }

    var showSaleDialog by remember { mutableStateOf(false) }
    var saleTarget by remember { mutableStateOf<com.example.jewels.data.local.model.InterestWithProduct?>(null) }
// ^ usa el tipo real que te devuelve observeAllWithProduct() (tu "row")


    val list by when (filter) {
        InterestFilter.ALL -> interestDao.observeAllWithProduct().collectAsState(initial = emptyList())
        InterestFilter.PENDING -> interestDao.observeByStatusWithProduct(InterestStatus.PENDING).collectAsState(initial = emptyList())
        InterestFilter.CONTACTED -> interestDao.observeByStatusWithProduct(InterestStatus.CONTACTED).collectAsState(initial = emptyList())
        InterestFilter.CLOSED -> interestDao.observeByStatusWithProduct(InterestStatus.CLOSED).collectAsState(initial = emptyList())
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Reservas / Interesados", style = MaterialTheme.typography.titleLarge)
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
            Text("No hay interesados para este filtro.")
        } else {
            LazyColumn {
                items(list, key = { it.id }) { row ->
                    ElevatedCard(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(row.buyerName, style = MaterialTheme.typography.titleMedium)
                            Text("Producto: ${row.productName}")
                            Text("Tel: ${row.phone}")
                            if (row.note.isNotBlank()) Text("Nota: ${row.note}")

                            Spacer(Modifier.height(8.dp))

                            // Estado: solo PENDING <-> CONTACTED (CLOSED no se toca manual)
                            AssistChip(
                                onClick = {
                                    if (row.status == InterestStatus.CLOSED) return@AssistChip

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
                                label = { Text("Estado: ${row.status.name}") }
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                OutlinedButton(
                                    onClick = {
                                        saleTarget = row
                                        showSaleDialog = true
                                    },
                                    enabled = row.status != InterestStatus.CLOSED
                                ) { Text("Registrar venta") }

                                OutlinedButton(
                                    onClick = {
                                        cancelTarget = row
                                        confirmCancelOpen = true
                                    },
                                    enabled = row.status != InterestStatus.CLOSED
                                ) { Text("Anular") }
                            }
                        }

                    }
                    }
                }
            }

            if (confirmCancelOpen && cancelTarget != null) {
                val target = cancelTarget!!

                AlertDialog(
                    onDismissRequest = {
                        confirmCancelOpen = false
                        cancelTarget = null
                    },
                    title = { Text("Anular reserva") },
                    text = {
                        Text("¿Seguro que quieres anular la reserva de ${target.buyerName} para “${target.productName}”? Se devolverá 1 unidad al stock.")
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


    }
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
                        label = { Text("Precio final (CLP)") }
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
                        val priceInt = price.toIntOrNull()
                        if (priceInt == null) return@TextButton

                        scope.launch(Dispatchers.IO) {
                            db.withTransaction {
                                // 1) crear venta
                                saleDao.insert(
                                    com.example.jewels.data.local.entity.SaleEntity(
                                        productId = row.productId,
                                        interestId = row.id,
                                        buyerName = row.buyerName,
                                        phone = row.phone,
                                        priceClp = priceInt,
                                        note = note
                                    )
                                )

                                // 2) cerrar reserva (CLOSED) automáticamente
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
fun AddInterestDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo interesado") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nombre") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Teléfono") })
                OutlinedTextField(note, { note = it }, label = { Text("Nota (opcional)") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) onSave(name, phone, note)
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}