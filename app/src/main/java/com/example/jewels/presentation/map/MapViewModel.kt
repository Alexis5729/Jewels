package com.example.jewels.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jewels.data.local.dao.AMBranchDao
import com.example.jewels.data.local.entity.BranchEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MapViewModel(
    branchDao: AMBranchDao
) : ViewModel() {

    val branches: StateFlow<List<BranchEntity>> =
        branchDao.observeActive()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
}
