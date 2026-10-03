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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.designsystem.DirectoryErrorState
import com.servacode.directory.core.designsystem.DirectoryGlyph
import com.servacode.directory.core.designsystem.DirectoryIcon
import com.servacode.directory.core.designsystem.DirectoryIcons
import com.servacode.directory.core.designsystem.DirectoryLoading
import com.servacode.directory.core.designsystem.DirectoryMenuDivider
import com.servacode.directory.core.designsystem.DirectoryMenuSection
import com.servacode.directory.core.designsystem.DirectoryOfflineNotice
import com.servacode.directory.core.designsystem.DirectoryPage
import com.servacode.directory.core.designsystem.DirectoryTheme
import com.servacode.directory.core.designsystem.DirectoryTopBar
import com.servacode.directory.core.designsystem.Space
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.feature.home.HomeLoad
import com.servacode.directory.feature.province.ProvinceScreen
import com.servacode.directory.feature.province.ProvinceViewModel

/**
 * The shell's two screens: the province, which is the shared screen itself (DECISION-095), and
 * that province's home, which reads the shared use case and draws with the shared design system
 * until Android's home moves to common code too (ROADMAP ٨).
 */
@Composable
internal fun ShellApp(graph: ShellGraph) {
    DirectoryTheme {
        val preferences by graph.preferences.values.collectAsState(initial = null)
        var choosing by remember { mutableStateOf(false) }
        val selected = preferences ?: return@DirectoryTheme
        val provinceId = selected.selectedProvinceId
        if (provinceId == null || choosing) {
            ShellProvince(
                graph,
                onChosen = { choosing = false },
                onBack = if (provinceId == null) null else ({ choosing = false }),
            )
        } else {
            HomeScreen(graph, provinceId, onChangeProvince = { choosing = true })
        }
    }
}

/**
 * The shared province picker (DECISION-095), its view model kept by the screen's owner for as long
 * as the app runs. A province already chosen can be gone back to.
 */
@Composable
private fun ShellProvince(graph: ShellGraph, onChosen: () -> Unit, onBack: (() -> Unit)? = null) {
    val model = viewModel { ProvinceViewModel(graph.provinces) }
    ProvinceScreen(model, onSelected = onChosen, onBack = onBack)
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
                HomeLoad.ProvinceRequired -> ShellProvince(graph, onChosen = {})
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
