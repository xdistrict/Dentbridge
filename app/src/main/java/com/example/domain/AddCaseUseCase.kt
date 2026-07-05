package com.example.domain

import com.example.DentalCase
import com.example.data.DentalRepository

/**
 * UseCase to handle the creation and registration of a new dental manufacturing case.
 */
class AddCaseUseCase(private val repository: DentalRepository) {
    suspend operator fun invoke(newCase: DentalCase) {
        // Enforce potential domain rules here before saving
        require(newCase.patientName.isNotBlank()) { "Patient name cannot be empty." }
        require(newCase.restorationType.isNotBlank()) { "Restoration type must be selected." }
        
        repository.addCase(newCase)
    }
}
