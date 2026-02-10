package com.example.jewels.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.jewels.presentation.inventory.InventoryScreen
import com.example.jewels.presentation.catalog.CatalogScreen
import com.example.jewels.presentation.map.MapScreen
import com.example.jewels.presentation.reservations.ReservationsScreen
import androidx.navigation.compose.*
import com.example.jewels.presentation.sales.SalesScreen

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
        BottomItem(Route.Sales, "Ventas", { Icon(Icons.Filled.ShoppingBag, null) }),
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
            composable(Route.Sales.path) { SalesScreen() }
        }
    }
}
