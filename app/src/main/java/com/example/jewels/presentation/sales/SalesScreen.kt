package com.example.jewels.presentation.sales

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.jewels.data.local.db.DbProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SalesScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }
    val saleDao = remember { db.saleDao() }

    val sales by saleDao.observeAllWithProduct().collectAsState(initial = emptyList())

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Ventas", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        if (sales.isEmpty()) {
            Text("Aún no hay ventas registradas.")
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
                        }
                    }
                }
            }
        }
    }
}

private fun formatDate(ms: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale("es","CL"))
    return sdf.format(Date(ms))
}
