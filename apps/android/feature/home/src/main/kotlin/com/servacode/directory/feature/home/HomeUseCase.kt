package com.servacode.directory.feature.home

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HomeUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<HomeLoad> = repository.load()
}
