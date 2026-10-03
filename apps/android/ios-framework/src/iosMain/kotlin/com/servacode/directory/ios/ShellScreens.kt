package com.servacode.directory.ios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryGlyph
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuGroup
import com.servacode.directory.core.designsystem.DirectoryMenuRow
import com.servacode.directory.core.designsystem.DirectoryMenuSection
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.feature.home.HomeLoad
import kotlinx.coroutines.launch

/**
 * The shell's two screens: the province, and that province's home. They read the shared use
 * cases as Android's view models do, and draw with the shared design system (DECISION-094); the
 * real screens replace them when Android's move to Compose Multiplatform (ROADMAP ٨).
 */
@Composable
internal fun ShellApp(graph: ShellGraph) {
    DirectoryTheme {
        val preferences by graph.preferences.values.collectAsState(initial = null)
        var choosing by remember { mutableStateOf(false) }
        val selected = preferences ?: return@DirectoryTheme
        val provinceId = selected.selectedProvinceId
        if (provinceId == null || choosing) {
            ProvinceScreen(graph, onChosen = { choosing = false })
        } else {
            HomeScreen(graph, provinceId, onChangeProvince = { choosing = true })
        }
    }
}

@Composable
private fun ProvinceScreen(graph: ShellGraph, onChosen: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by remember(attempt) { graph.provinces.provinces() }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    DirectoryPage(topBar = { DirectoryTopBar(title = ShellWord.CHOOSE_PROVINCE.text()) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = loaded) {
                null -> DirectoryLoading()
                is Loaded.Failed -> DirectoryErrorState(
                    title = ShellWord.LOAD_FAILED.text(),
                    error = state.error,
                    onRetry = { attempt++ },
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(Space.base),
                    verticalArrangement = Arrangement.spacedBy(Space.md),
                ) {
                    if (state is Loaded.Stale) {
                        item { DirectoryOfflineNotice(text = ShellWord.STALE_LIST.text(), onRetry = { attempt++ }) }
                    }
                    item {
                        DirectoryMenuGroup {
                            state.valueOrNull().orEmpty().forEachIndexed { index, province ->
                                if (index > 0) DirectoryMenuDivider()
                                DirectoryMenuRow(
                                    title = province.nameAr,
                                    icon = DirectoryIcons.pin,
                                    onClick = {
                                        scope.launch {
                                            graph.provinces.select(province.id)
                                            onChosen()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(graph: ShellGraph, provinceId: String, onChangeProvince: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val load by remember(provinceId, attempt) { graph.home() }.collectAsState(initial = null)
    val snapshot = (load as? HomeLoad.Snapshot)?.loaded?.valueOrNull()
    DirectoryPage(
        topBar = {
            DirectoryTopBar(
                title = snapshot?.province?.nameAr ?: ShellWord.APP_NAME.text(),
                actionIcon = DirectoryIcons.pin,
                actionLabel = ShellWord.CHANGE_PROVINCE.text(),
                onAction = onChangeProvince,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = load) {
                null -> DirectoryLoading()
                HomeLoad.ProvinceRequired -> ProvinceScreen(graph, onChosen = {})
                is HomeLoad.Snapshot -> when (val loaded = state.loaded) {
                    is Loaded.Failed -> DirectoryErrorState(
                        title = ShellWord.LOAD_FAILED.text(),
                        error = loaded.error,
                        onRetry = { attempt++ },
                    )
                    else -> HomeLists(
                        snapshot = loaded.valueOrNull() ?: return@Box,
                        stale = loaded is Loaded.Stale,
                        onRetry = { attempt++ },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLists(snapshot: HomeSnapshot, stale: Boolean, onRetry: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(Space.base),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        if (stale) item { DirectoryOfflineNotice(text = ShellWord.STALE_HOME.text(), onRetry = onRetry) }
        homeSections(snapshot).forEach { section ->
            item(key = section.title.key) {
                DirectoryMenuSection(label = section.title.text()) {
                    section.rows.forEachIndexed { index, row ->
                        if (index > 0) DirectoryMenuDivider()
                        InfoRow(row)
                    }
                }
            }
        }
    }
}

/** A row that says something and leads nowhere yet: the shell has no facility page. */
@Composable
private fun InfoRow(row: HomeRow) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.base, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        DirectoryIcon(row.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f)) {
            Text(row.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (row.detail != null) {
                Text(
                    row.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun <T> Loaded<T>.valueOrNull(): T? = when (this) {
    is Loaded.Cached -> value
    is Loaded.Fresh -> value
    is Loaded.Stale -> value
    is Loaded.Failed -> null
}

/** One row of the home: a name, what it is beneath it, and its section's mark. */
internal data class HomeRow(val id: String, val text: String, val detail: String?, val icon: DirectoryGlyph)

internal data class HomeSection(val title: ShellWord, val rows: List<HomeRow>)

/** What the home lists, in order; a section with nothing in it is left out. */
internal fun homeSections(snapshot: HomeSnapshot): List<HomeSection> {
    fun FacilitySummary.row() = HomeRow(id, nameAr, category.nameAr, DirectoryIcons.category(category.iconKey))
    return listOf(
        HomeSection(ShellWord.DUTY_NOW, snapshot.dutyNow.map { it.row() }),
        HomeSection(ShellWord.OPEN_NOW, snapshot.openNearby.map { it.row() }),
        HomeSection(ShellWord.NEARBY, snapshot.nearby.map { it.row() }),
        HomeSection(
            ShellWord.CATEGORIES,
            snapshot.categories.map { HomeRow(it.id, it.nameAr, null, DirectoryIcons.category(it.iconKey)) },
        ),
    ).filter { it.rows.isNotEmpty() }
}
