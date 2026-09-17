package com.servacode.directory.feature.home

import javax.inject.Inject

class HomeUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(): HomeLoadResult = repository.load()
}
