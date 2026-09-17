package com.servacode.directory.feature.directory

import javax.inject.Inject

class DirectoryUseCase @Inject constructor(
    private val repository: DirectoryRepository,
) {
    suspend operator fun invoke(categoryId: String, filter: DirectoryFilter): DirectoryLoadResult =
        repository.load(categoryId, filter)
}
