package com.servacode.directory.feature.province

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.Province
import kotlinx.coroutines.flow.Flow

class ProvinceUseCase @Inject constructor(
    private val repository: ProvinceRepository,
) {
    fun provinces(): Flow<Loaded<List<Province>>> = repository.provinces()
    suspend fun select(id: String) = repository.select(id)
}
