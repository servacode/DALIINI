package com.servacode.directory.feature.province

import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.Province
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProvinceUseCase @Inject constructor(
    private val repository: ProvinceRepository,
) {
    fun provinces(): Flow<Loaded<List<Province>>> = repository.provinces()
    suspend fun select(id: String) = repository.select(id)
}
