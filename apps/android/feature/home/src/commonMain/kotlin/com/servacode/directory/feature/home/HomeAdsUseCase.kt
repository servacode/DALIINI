package com.servacode.directory.feature.home

import com.servacode.directory.core.inject.Inject
import com.servacode.directory.core.model.HomeAd
import kotlinx.coroutines.flow.Flow

class HomeAdsUseCase @Inject constructor(
    private val repository: HomeAdsRepository,
) {
    operator fun invoke(provinceId: String): Flow<List<HomeAd>> = repository.load(provinceId)
}
