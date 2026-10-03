package com.servacode.directory.ios

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.servacode.directory.core.database.Loaded
import com.servacode.directory.core.model.AppError
import com.servacode.directory.core.model.FacilitySummary
import com.servacode.directory.core.model.HomeSnapshot
import com.servacode.directory.core.model.Province
import com.servacode.directory.designsystem.generated.DirectoryTokens
import com.servacode.directory.feature.home.HomeLoad
import kotlinx.coroutines.launch

/**
 * The shell's two screens: the province, and that province's home. They read the shared use
 * cases exactly as Android's view models do; the real screens replace them when Android's move
 * to Compose Multiplatform (ROADMAP ٨).
 */
@Composable
internal fun ShellApp(graph: ShellGraph) {
    val preferences by graph.preferences.values.collectAsState(initial = null)
    var choosing by remember { mutableStateOf(false) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(Modifier.fillMaxSize().background(Palette.background).safeDrawingPadding()) {
            val selected = preferences ?: return@Box
            val provinceId = selected.selectedProvinceId
            if (provinceId == null || choosing) {
                ProvinceScreen(graph, onChosen = { choosing = false })
            } else {
                HomeScreen(graph, provinceId, onChangeProvince = { choosing = true })
            }
        }
    }
}

@Composable
private fun ProvinceScreen(graph: ShellGraph, onChosen: () -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val loaded by remember(attempt) { graph.provinces.provinces() }.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Bar(ShellWord.CHOOSE_PROVINCE.text())
        when (val state = loaded) {
            null -> Note(ShellWord.LOADING.text())
            is Loaded.Failed -> Failure(state.error) { attempt++ }
            else -> {
                if (state is Loaded.Stale) Note(ShellWord.STALE_LIST.text())
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.valueOrEmpty(), key = Province::id) { province ->
                        ListRow(province.nameAr) {
                            scope.launch {
                                graph.provinces.select(province.id)
                                onChosen()
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
    Column(Modifier.fillMaxSize()) {
        when (val state = load) {
            null -> {
                Bar(ShellWord.APP_NAME.text())
                Note(ShellWord.LOADING.text())
            }
            HomeLoad.ProvinceRequired -> ProvinceScreen(graph, onChosen = {})
            is HomeLoad.Snapshot -> when (val loaded = state.loaded) {
                is Loaded.Failed -> {
                    Bar(ShellWord.APP_NAME.text(), ShellWord.CHANGE_PROVINCE.text(), onChangeProvince)
                    Failure(loaded.error) { attempt++ }
                }
                else -> {
                    val snapshot = loaded.valueOrNull() ?: return@Column
                    Bar(snapshot.province.nameAr, ShellWord.CHANGE_PROVINCE.text(), onChangeProvince)
                    if (loaded is Loaded.Stale) Note(ShellWord.STALE_HOME.text())
                    HomeLists(snapshot)
                }
            }
        }
    }
}

@Composable
private fun HomeLists(snapshot: HomeSnapshot) {
    LazyColumn(Modifier.fillMaxSize()) {
        homeSections(snapshot).forEach { section ->
            item(key = "title:${section.title.key}") { SectionTitle(section.title.text()) }
            items(section.rows, key = { "${section.title.key}:${it.id}" }) { row -> ListRow(row.text, row.detail) }
        }
    }
}

@Composable
private fun Bar(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().background(Palette.bar).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(title, style = TextStyle(color = Palette.onBar, fontSize = 20.sp, fontWeight = FontWeight.Bold))
        if (action != null) {
            BasicText(
                action,
                modifier = Modifier.clickable(onClick = onAction).padding(8.dp),
                style = TextStyle(color = Palette.onBarMuted, fontSize = 14.sp),
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    BasicText(
        text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        style = TextStyle(color = Palette.primary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun ListRow(text: String, detail: String? = null, onClick: (() -> Unit)? = null) {
    val base = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(Palette.surface)
    Column((if (onClick != null) base.clickable(onClick = onClick) else base).padding(16.dp)) {
        BasicText(text, style = TextStyle(color = Palette.text, fontSize = 17.sp))
        if (detail != null) {
            BasicText(detail, style = TextStyle(color = Palette.textSecondary, fontSize = 14.sp))
        }
    }
}

@Composable
private fun Note(text: String) {
    BasicText(
        text,
        modifier = Modifier.fillMaxWidth().background(Palette.surfaceAlt).padding(16.dp),
        style = TextStyle(color = Palette.textSecondary, fontSize = 14.sp),
    )
}

@Composable
private fun Failure(error: AppError, onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(failureWord(error).text(), style = TextStyle(color = Palette.text, fontSize = 16.sp))
        BasicText(
            ShellWord.RETRY.text(),
            modifier = Modifier.padding(top = 16.dp).clickable(onClick = onRetry).padding(12.dp),
            style = TextStyle(color = Palette.primary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
        )
    }
}

/** The brand's colours, from the same generated tokens as the site and Android. */
private object Palette {
    val background = DirectoryTokens.ColorsBackground.toColor()
    val surface = DirectoryTokens.ColorsSurface.toColor()
    val surfaceAlt = DirectoryTokens.ColorsSurfaceAlt.toColor()
    val primary = DirectoryTokens.ColorsPrimary.toColor()
    val bar = DirectoryTokens.ColorsBarDeep.toColor()
    val onBar = DirectoryTokens.ColorsBarContent.toColor()
    val onBarMuted = DirectoryTokens.ColorsBarContentMuted.toColor()
    val text = DirectoryTokens.ColorsTextPrimary.toColor()
    val textSecondary = DirectoryTokens.ColorsTextSecondary.toColor()
}

internal fun String.toColor(): Color = Color(0xFF000000 or removePrefix("#").toLong(16))

private fun <T> Loaded<T>.valueOrNull(): T? = when (this) {
    is Loaded.Cached -> value
    is Loaded.Fresh -> value
    is Loaded.Stale -> value
    is Loaded.Failed -> null
}

private fun Loaded<List<Province>>.valueOrEmpty(): List<Province> = valueOrNull().orEmpty()

/** One row of the home: a facility's name, and its category beneath it. */
internal data class HomeRow(val id: String, val text: String, val detail: String?)

internal data class HomeSection(val title: ShellWord, val rows: List<HomeRow>)

/** What the home lists, in order; a section with nothing in it is left out. */
internal fun homeSections(snapshot: HomeSnapshot): List<HomeSection> {
    fun FacilitySummary.row() = HomeRow(id, nameAr, category.nameAr)
    return listOf(
        HomeSection(ShellWord.DUTY_NOW, snapshot.dutyNow.map { it.row() }),
        HomeSection(ShellWord.OPEN_NOW, snapshot.openNearby.map { it.row() }),
        HomeSection(ShellWord.NEARBY, snapshot.nearby.map { it.row() }),
        HomeSection(ShellWord.CATEGORIES, snapshot.categories.map { HomeRow(it.id, it.nameAr, null) }),
    ).filter { it.rows.isNotEmpty() }
}
