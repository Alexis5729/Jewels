package com.example.jewels.presentation.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.jewels.data.local.db.DbProvider
import com.example.jewels.data.local.entity.BranchEntity
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun MapScreen() {

    val context = LocalContext.current

    val db = remember { DbProvider.get(context) }

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