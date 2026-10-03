package com.servacode.directory.feature.bootstrap

import com.servacode.directory.core.inject.Inject

class BootstrapUseCase @Inject constructor(
    private val repository: BootstrapRepository,
) {
    suspend operator fun invoke(): BootstrapResult = repository.initialize()
}
