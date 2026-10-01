package com.servacode.directory.feature.home

import com.servacode.directory.core.model.HomeAd
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeAdsUseCase @Inject constructor(
    private val repository: HomeAdsRepository,
) {
    operator fun invoke(provinceId: String): Flow<List<HomeAd>> = repository.load(provinceId)
}
