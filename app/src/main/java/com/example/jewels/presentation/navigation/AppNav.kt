package com.example.jewels.presentation.navigation

import androidx.compose.foundation.layout.*
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
import com.example.jewels.data.local.db.AppDatabase
import com.example.jewels.data.local.entity.BranchEntity
import com.example.jewels.presentation.map.MapViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
private fun InventoryScreen() = Surface { Text("Inventario (admin)", modifier = Modifier.padding(16.dp)) }

@Composable
private fun CatalogScreen() = Surface { Text("Catálogo (vitrina)", modifier = Modifier.padding(16.dp)) }

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


@Composable
private fun ReservationsScreen() = Surface { Text("Reservas / Interesados", modifier = Modifier.padding(16.dp)) }

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
