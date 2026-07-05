package com.example.ui.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.CaseStatus
import com.example.DentalCase
import com.example.data.DentalRepository
import com.example.domain.GetCasesUseCase
import com.example.domain.AddCaseUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Split ViewModel focusing entirely on Dental Cases management.
 * Leverages Repository and domain UseCases.
 */
class CaseViewModel(
    private val repository: DentalRepository,
    private val getCasesUseCase: GetCasesUseCase,
    private val addCaseUseCase: AddCaseUseCase
) : ViewModel() {

    private val TAG = "CaseViewModel"

    private val _cases = MutableStateFlow<List<DentalCase>>(emptyList())
    val cases: StateFlow<List<DentalCase>> = _cases.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadCases()
    }

    private fun loadCases() {
        getCasesUseCase()
            .onEach { list ->
                _cases.value = list
            }
            .catch { e ->
                Log.e(TAG, "Error fetching cases", e)
            }
            .launchIn(viewModelScope)
    }

    fun createCase(newCase: DentalCase) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                addCaseUseCase(newCase)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create case", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun updateCaseStatus(caseId: String, newStatus: CaseStatus) {
        viewModelScope.launch {
            try {
                repository.updateCaseStatus(caseId, newStatus)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update status", e)
            }
        }
    }
}
