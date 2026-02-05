package com.example.jewels.presentation.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.room.Room
import com.example.jewels.data.local.entity.ProductEntity
import com.example.jewels.data.local.entity.ProductStatus
import com.example.jewels.data.local.entity.BranchEntity
import com.example.jewels.data.local.entity.InterestEntity
import com.example.jewels.data.local.entity.InterestStatus
import androidx.compose.material3.HorizontalDivider
import com.example.jewels.data.local.db.AppDatabase
import com.example.jewels.presentation.map.MapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.ProductPhotoEntity
import com.example.jewels.data.local.model.ProductWithPhotos
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import android.net.Uri
import kotlin.toString


private data class BottomItem(
    val route: Route,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AppScaffold() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val items = listOf(
        BottomItem(Route.Inventory, "Inventario", { Icon(Icons.Filled.Inventory2, contentDescription = null) }),
        BottomItem(Route.Catalog, "Catálogo", { Icon(Icons.Filled.ShoppingBag, contentDescription = null) }),
        BottomItem(Route.Map, "Mapa", { Icon(Icons.Filled.Map, contentDescription = null) }),
        BottomItem(Route.Reservations, "Reservas", { Icon(Icons.Filled.Bookmark, contentDescription = null) }),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                items.forEach { item ->
                    val selected = currentRoute == item.route.path
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route.path) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = item.icon,
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Inventory.path,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Inventory.path) { InventoryScreen() }
            composable(Route.Catalog.path) { CatalogScreen() }
            composable(Route.Map.path) { MapScreen() }
            composable(Route.Reservations.path) { ReservationsScreen() }
        }
    }
}

@Composable
private fun InventoryScreen() {
    val context = LocalContext.current
    val db = remember { DbProvider.get(context) }   // ✅ misma instancia en toda la app
    val dao = remember { db.productDao() }
    val scope = rememberCoroutineScope()

    val products by dao.observeAllWithPhotos().collectAsState(initial = emptyList())
    var selectedProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text("Inventario", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            if (products.isEmpty()) {
                Text("Aún no hay productos. Toca + para agregar.")
            } else {
                LazyColumn {
                    items(products, key = { it.product.id }) { item ->
                        val p = item.product
                        val photos = item.photos

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .clickable { selectedProduct = p }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp), // ✅ padding interno
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val thumb = photos.firstOrNull()?.uri
                                if (thumb != null) {
                                    AsyncImage(
                                        model = android.net.Uri.parse(thumb),
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                }

                                Column(Modifier.weight(1f)) {
                                    Text(p.name, style = MaterialTheme.typography.titleMedium)
                                    Text("Precio: $${p.priceClp} CLP")
                                    Text("Stock: ${p.stock}")
                                    Text(
                                        if (p.status == ProductStatus.AVAILABLE)
                                            "Estado: Disponible"
                                        else
                                            "Estado: Agotado"
                                    )
                                    Text("Fotos: ${photos.size}")
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
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
                    scope.launch(Dispatchers.IO) {
                        dao.update(updated.copy(updatedAt = System.currentTimeMillis()))
                    }
                    selectedProduct = null
                },
                onDelete = { prod ->
                    scope.launch(Dispatchers.IO) { dao.delete(prod) }
                    selectedProduct = null
                }
            )
        }
    }
}

private enum class CatalogFilter { ALL, AVAILABLE, SOLD_OUT }

@Composable
private fun CatalogScreen() {
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

    @Composable
    private fun MapScreen() {

        val context = LocalContext.current

        val db = remember {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "jewels_db"
            )
                .fallbackToDestructiveMigration()
                .build()
        }

        val viewModel = remember {
            MapViewModel(db.branchDao())
        }

        val branches by viewModel.branches.collectAsState()

        var showForm by remember { mutableStateOf(false) }

        val scope = rememberCoroutineScope()

        var pickedLatLng by remember { mutableStateOf<LatLng?>(null) }


        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(-33.45, -70.66),
                11f
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // MAPA
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng ->
                    pickedLatLng = latLng
                    showForm = true
                }

            ) {
                branches.forEach { branch ->
                    Marker(
                        state = MarkerState(
                            LatLng(branch.lat, branch.lng)
                        ),
                        title = branch.name,
                        snippet = branch.address
                    )
                }
            }

            // BOTÓN +
            FloatingActionButton(
                onClick = { showForm = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar sucursal")
            }

            // FORMULARIO
            if (showForm) {
                AddBranchDialog(
                    initialLatLng = pickedLatLng,
                    onDismiss = {
                        showForm = false
                        pickedLatLng = null
                    },
                    onSave = { branch ->
                        scope.launch(Dispatchers.IO) {
                            db.branchDao().insert(branch)
                        }
                        showForm = false
                        pickedLatLng = null
                    }
                )
            }
        }
    }

private enum class InterestFilter { ALL, PENDING, CONTACTED, CLOSED }
@Composable
private fun ReservationsScreen() {
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
    private fun AddBranchDialog(
        initialLatLng: LatLng?,
        onDismiss: () -> Unit,
        onSave: (BranchEntity) -> Unit
    ) {

        var name by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var lat by remember { mutableStateOf(initialLatLng?.latitude?.toString() ?: "") }
        var lng by remember { mutableStateOf(initialLatLng?.longitude?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val latD = lat.toDoubleOrNull()
                        val lngD = lng.toDoubleOrNull()

                        if (name.isNotBlank() && latD != null && lngD != null) {
                            onSave(
                                BranchEntity(
                                    name = name,
                                    address = address,
                                    lat = latD,
                                    lng = lngD
                                )
                            )
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            },
            title = { Text("Nueva sucursal") },

            text = {
                Column {

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") }
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Dirección") }
                    )

                    OutlinedTextField(
                        value = lat,
                        onValueChange = { lat = it },
                        label = { Text("Latitud") }
                    )

                    OutlinedTextField(
                        value = lng,
                        onValueChange = { lng = it },
                        label = { Text("Longitud") }
                    )
                }
            }
        )
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
                        label = { Text("Nombre") }
                    )

                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Descripción") }
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it.filter { c -> c.isDigit() } },
                        label = { Text("Precio (CLP)") }
                    )

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { c -> c.isDigit() } },
                        label = { Text("Stock") }
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = available,
                            onCheckedChange = { available = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (available) "Disponible" else "Agotado")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val priceInt = price.toIntOrNull()
                        val stockInt = stock.toIntOrNull()

                        if (name.isNotBlank() && priceInt != null && stockInt != null) {
                            onSave(
                                ProductEntity(
                                    name = name,
                                    description = desc,
                                    priceClp = priceInt,
                                    stock = stockInt,
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

    val interestDao = remember { db.interestDao() }
    val interests by interestDao.observeByProduct(product.id).collectAsState(initial = emptyList())
    var showAddInterest by remember { mutableStateOf(false) }

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar producto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") }
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") }
                )

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it.filter { c -> c.isDigit() } },
                    label = { Text("Precio (CLP)") }
                )

                OutlinedTextField(
                    value = stock,
                    onValueChange = { stock = it.filter { c -> c.isDigit() } },
                    label = { Text("Stock") }
                )

                // --- Fotos (preview simple) ---
                Text("Fotos: ${photos.size}")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    photos.take(3).forEach { ph ->
                        AsyncImage(
                            model = Uri.parse(ph.uri),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { pickImageLauncher.launch(arrayOf("image/*")) }
                ) {
                    Text("Agregar foto desde galería")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = available, onCheckedChange = { available = it })
                    Spacer(Modifier.width(8.dp))
                    Text(if (available) "Disponible" else "Agotado")
                }

                HorizontalDivider()

                Text("Interesados: ${interests.size}")

                if (interests.isEmpty()) {
                    Text("Aún no hay interesados para este producto.")
                } else {
                    interests.take(3).forEach { itx ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text(itx.buyerName, style = MaterialTheme.typography.titleSmall)
                                Text("Tel: ${itx.phone}")
                                if (itx.note.isNotBlank()) Text("Nota: ${itx.note}")

                                Spacer(Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = {
                                            val next = when (itx.status) {
                                                InterestStatus.PENDING -> InterestStatus.CONTACTED
                                                InterestStatus.CONTACTED -> InterestStatus.CLOSED
                                                InterestStatus.CLOSED -> InterestStatus.PENDING
                                            }
                                            scope.launch(Dispatchers.IO) {
                                                interestDao.update(itx.copy(status = next))
                                            }
                                        },
                                        label = { Text("Estado: ${itx.status.name}") }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Text("Mostrando 3 últimos (ver todo en “Reservas”)")
                }

                OutlinedButton(onClick = { showAddInterest = true }) {
                    Text("Agregar interesado")
                }

            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = price.toIntOrNull()
                    val s = stock.toIntOrNull()
                    if (p != null && s != null && name.isNotBlank()) {
                        onSave(
                            product.copy(
                                name = name,
                                description = desc,
                                priceClp = p,
                                stock = s,
                                status = if (available) ProductStatus.AVAILABLE else ProductStatus.SOLD_OUT,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDelete(product) }) { Text("Eliminar") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
    if (showAddInterest) {
        AddInterestDialog(
            onDismiss = { showAddInterest = false },
            onSave = { name, phone, note ->
                scope.launch(Dispatchers.IO) {
                    interestDao.insert(
                        InterestEntity(
                            productId = product.id,
                            buyerName = name,
                            phone = phone,
                            note = note
                        )
                    )
                }
                showAddInterest = false
            }
        )
    }
}

@Composable
private fun AddInterestDialog(
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

