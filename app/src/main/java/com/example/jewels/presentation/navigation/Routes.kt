package com.example.jewels.presentation.navigation

sealed class Route(val path: String) {
    data object Inventory : Route("inventory")
    data object Catalog : Route("catalog")
    data object Map : Route("map")
    data object Reservations : Route("reservations")

    object Sales : Route("sales")

}
