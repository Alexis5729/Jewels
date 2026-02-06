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
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private enum class InterestFilter { ALL, PENDING, CONTACTED, CLOSED }

@Composable
fun ReservationsScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val interestDao = remember { db.interestDao() }
    val scope = rememberCoroutineScope()

    var filter by remember { mutableStateOf(InterestFilter.ALL) }

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

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = {
                                        val next = when (row.status) {
                                            InterestStatus.PENDING -> InterestStatus.CONTACTED
                                            InterestStatus.CONTACTED -> InterestStatus.CLOSED
                                            InterestStatus.CLOSED -> InterestStatus.PENDING
                                        }
                                        scope.launch(Dispatchers.IO) {
                                            // actualizamos con entidad mínima
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
                            }
                        }
                    }
                }
            }
        }
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