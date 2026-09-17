package com.servacode.directory.feature.map

import com.servacode.directory.core.model.PublicMapFacility
import javax.inject.Inject

class MapUseCase @Inject constructor(
    private val repository: MapRepository,
) {
    suspend operator fun invoke(viewport: MapViewport): Result<List<PublicMapFacility>> = repository.load(viewport)
}
