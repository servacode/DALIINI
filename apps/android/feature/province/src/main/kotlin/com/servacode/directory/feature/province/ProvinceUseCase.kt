package com.servacode.directory.feature.province

import com.servacode.directory.core.model.Province
import javax.inject.Inject

class ProvinceUseCase @Inject constructor(
    private val repository: ProvinceRepository,
) {
    suspend fun load(): Pair<List<Province>, Boolean> = repository.load()
    suspend fun select(id: String) = repository.select(id)
}
