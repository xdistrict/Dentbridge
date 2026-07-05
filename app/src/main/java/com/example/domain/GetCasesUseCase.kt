package com.example.domain

import com.example.DentalCase
import com.example.data.DentalRepository
import kotlinx.coroutines.flow.Flow

/**
 * UseCase to retrieve dental cases, abstracting business logic from the ViewModel.
 */
class GetCasesUseCase(private val repository: DentalRepository) {
    operator fun invoke(): Flow<List<DentalCase>> {
        return repository.getCasesFlow()
    }
}
