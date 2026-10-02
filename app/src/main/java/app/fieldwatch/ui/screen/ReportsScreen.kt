package app.fieldwatch.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import app.fieldwatch.ui.component.FieldwatchActionButton
import androidx.compose.material3.Scaffold
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldwatch.domain.MacUtil
import app.fieldwatch.ui.RadioClassBadge
import app.fieldwatch.ui.RadioKindMark
import app.fieldwatch.domain.LogExportKind
import app.fieldwatch.domain.LogExportRadios
import app.fieldwatch.domain.Sit
import app.fieldwatch.domain.SitDiff
import app.fieldwatch.domain.SitPathPlot
import app.fieldwatch.ui.component.AircraftAmber
import app.fieldwatch.ui.component.FieldwatchDropdownField
import app.fieldwatch.ui.component.SitPathCanvas
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.component.FieldwatchSwitch
import app.fieldwatch.ui.component.SectionCard
import app.fieldwatch.ui.theme.Cyan
import app.fieldwatch.ui.theme.LocalNightMode
import app.fieldwatch.ui.theme.nightIf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: FieldwatchUi,
    vm: FieldwatchViewModel,
    exporting: Boolean,
    onSaveToStorage: () -> Unit,
    onSaveSitToStorage: () -> Unit,
    onSignatureCandidates: () -> Unit,
    onOpenPathRadio: (String) -> Unit = {},
) {
    val settings = state.settings
    var confirmClear by remember { mutableStateOf(false) }
    var startSit by remember { mutableStateOf(false) }
    var sitNameDraft by remember { mutableStateOf("") }
    var renameSitId by remember { mutableStateOf<String?>(null) }
    var renameDraft by remember { mutableStateOf("") }
    var deleteSitId by remember { mutableStateOf<String?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = { NestedTopBar("报告与监测") },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.demoMode) {
                Text(
                    "隐私演示模式处于开启状态。态势研判、监测期对比、AI 导出以及详情分享中的 MAC 地址后半部分脱敏为 **:**:**。GPS 坐标已遮蔽。日志文件、监测期导出及 GPX / KML / WiGLE 导出文件仍保留完整物理地址与经纬度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            SectionCard("监测期 (Sit)") {
                Text(
                    "“监测期”是指在此处侦听到的无线电设备命名时间窗口。下方的选择将驱动轨迹、态势研判报告以及对比基准：当前进行中的监测期、选中的历史监测期，若未手动开启则默认采用最近 15 分钟。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val open = state.sit.open
                if (open != null) {
                    val dur = Sit.fmtDuration(open.durationMs())
                    Text(
                        "当前监测期: ${open.name} · $dur · ${state.sit.radioCount} 台设备",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    FieldwatchActionButton(
                        onClick = vm::endSit,
                        enabled = !exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("结束监测期") }
                } else {
                    FieldwatchActionButton(
                        onClick = {
                            sitNameDraft = vm.defaultSitName()
                            startSit = true
                        },
                        enabled = !exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("开启新监测期") }
                    Text(
                        if (state.sit.closed.isEmpty()) {
                            "当前没有进行中的监测期。可在此处开启新监测期；在此之前轨迹与研判报告默认显示最近 15 分钟数据。"
                        } else {
                            "当前没有进行中的监测期。可在此处开启新监测期；轨迹与研判报告将采用下方选中的监测期。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.sit.closed.isEmpty() && open == null) {
                    Text(
                        "暂无保存的历史监测期。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.sit.closed.isNotEmpty()) {
                    val pickEnabled = open == null && !exporting
                    SitChoiceRow(
                        selected = state.sit.selectedId == null,
                        enabled = pickEnabled,
                        title = "最近 15 分钟",
                        subtitle = "轨迹与研判直接使用内存数据，非已存监测期。",
                        onSelect = { vm.selectSit(null) },
                    )
                    state.sit.closed.forEach { row ->
                        val dur = Sit.fmtDuration(row.durationMs())
                        val extra = if (row.extraAttentionCount > 0) {
                            " · 重点关注 ${row.extraAttentionCount}"
                        } else {
                            ""
                        }
                        SitChoiceRow(
                            selected = state.sit.selectedId == row.id,
                            enabled = pickEnabled,
                            title = row.name,
                            subtitle = "${Sit.defaultName(row.startAt)} · $dur · ${row.radioCount} 台设备$extra",
                            onSelect = { vm.selectSit(row.id) },
                        )
                    }
                    if (open != null) {
                        Text(
                            "结束当前监测期后方可为轨迹与研判选择历史监测期。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val picked = state.sit.closed.firstOrNull { it.id == state.sit.selectedId }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FieldwatchActionButton(
                            onClick = {
                                if (picked != null) {
                                    renameSitId = picked.id
                                    renameDraft = picked.name
                                }
                            },
                            enabled = !exporting && picked != null,
                            modifier = Modifier.weight(1f),
                        ) { Text("重命名") }
                        FieldwatchActionButton(
                            onClick = { if (picked != null) deleteSitId = picked.id },
                            enabled = !exporting && picked != null,
                            modifier = Modifier.weight(1f),
                        ) { Text("删除") }
                    }
                    FieldwatchActionButton(
                        onClick = { confirmDeleteAll = true },
                        enabled = !exporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("删除所有历史监测期") }
                }
            }

            val pathModel by vm.sitPath.collectAsStateWithLifecycle()
            LaunchedEffect(state.sit.selectedId, state.sit.open?.id) {
                while (true) {
                    vm.refreshSitPath()
                    kotlinx.coroutines.delay(3_000L)
                }
            }
            SectionCard("移动轨迹") {
                Text(
                    "正北朝上。轨迹线表示本机移动路线。黑点为起点。蓝点为当前最新位置。MAC 或特征警报显示为单个类别图标。已解码经纬度使用该无线电设备发送的最新位置。数字表示同一点汇聚的多个设备。粗绿线表示停留。沿轨迹标有时间刻度。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val model = pathModel
                val showWalk = model != null && model.emptyHint == null
                val showAircraft = model != null && model.aircraftCards.isNotEmpty()
                if (model == null || (!showWalk && !showAircraft)) {
                    Text(
                        model?.emptyHint ?: "开启 GPS 定位标记并移动，或打开记录有轨迹的监测期。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val pathTiles by vm.pathTiles.collectAsStateWithLifecycle()
                    val aircraftTiles by vm.pathAircraftTiles.collectAsStateWithLifecycle()
                    if (!showWalk) {
                        Text(
                            model.emptyHint ?: "开启 GPS 定位标记并移动，或打开记录有轨迹的监测期。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showWalk) {
                    val stopN = model.dots.size
                    Text(
                        buildString {
                            append("${model.title} · 轨迹长 ${model.lengthM.toInt()} 米 · 跨度 ${model.spanM.toInt()} 米")
                            if (stopN > 0) {
                                append(" · $stopN 次警报")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    SitPathCanvas(model, tiles = pathTiles, onOpenRadio = onOpenPathRadio)
                    Text(
                        "点击数字可查看该处的设备列表。点击单个图标查看该设备。再次点击关闭。点击列表中或下方的任意一行可打开该设备详情。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (model.craft.isNotEmpty() || model.pilots.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (model.craft.isNotEmpty()) {
                                val multi = model.craft.any { it.samples.size >= 2 }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    if (multi) AdvertisedTrackSwatch() else AdvertisedRingSwatch()
                                    Text(
                                        if (multi) {
                                            "= 此轨迹 2 公里范围内的广播航迹"
                                        } else {
                                            "= 此轨迹 2 公里范围内的单次广播定位"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                            if (model.pilots.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    PilotSwatch()
                                    Text(
                                        "= 飞手",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    val alertsOnACard = model.aircraftCards.any { it.dots.isNotEmpty() }
                    if (model.dots.isEmpty() && !alertsOnACard) {
                        Text(
                            "此轨迹上无带有 GPS 标记的 MAC 或特征警报。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (model.dots.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            model.dots.forEachIndexed { i, dot ->
                                PathRadioRow(
                                    index = i + 1,
                                    dot = dot,
                                    demoMode = settings.demoMode,
                                    onOpen = { onOpenPathRadio(dot.key) },
                                )
                            }
                        }
                    }
                    }
                    model.aircraftCards.forEachIndexed { index, card ->
                        val fixes = card.craft.sumOf { it.samples.size }
                        Text(
                            card.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = AircraftAmber,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Text(
                            buildString {
                                append("$fixes 次广播定位")
                                if (card.lengthM >= 1.0) append(" · ${card.lengthM.toInt()} 米")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (card.dots.isNotEmpty()) {
                            Text(
                                "${card.dots.size} 次警报",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SitPathCanvas(
                            card,
                            tiles = aircraftTiles.getOrElse(index) { emptyList() },
                            onOpenRadio = onOpenPathRadio,
                        )
                        if (card.dots.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                card.dots.forEachIndexed { i, dot ->
                                    PathRadioRow(
                                        index = i + 1,
                                        dot = dot,
                                        demoMode = settings.demoMode,
                                        onOpen = { onOpenPathRadio(dot.key) },
                                    )
                                }
                            }
                        }
                        if (card.caption.isNotBlank()) {
                            Text(
                                card.caption,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (model.looseAdvertised > 0) {
                        Text(
                            "无 UAS ID 的广播定位已包含在监测期态势研判报告中。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            SectionCard("态势研判报告") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FieldwatchActionButton(
                    onClick = vm::startFieldDebrief,
                    enabled = !exporting,
                    modifier = Modifier.weight(1f),
                ) { Text("态势研判 (文本)") }
                FieldwatchActionButton(
                    onClick = vm::startFieldDebriefPdf,
                    enabled = !exporting,
                    modifier = Modifier.weight(1f),
                ) { Text("态势研判 (PDF)") }
            }
            Text(
                sitReportCaption(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "显示未匹配的轮换 MAC BLE",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                FieldwatchSwitch(
                    settings.debriefShowUnmatchedRandomBle,
                    { on -> vm.updateSettings { it.copy(debriefShowUnmatchedRandomBle = on) } },
                )
            }
            Text(
                "关闭（默认）：研判报告文本/PDF 清单将跳过未匹配的随机 BLE 设备。统计总数仍包含它们。重点关注、命名特征、收藏书签及固定载荷设备均会保留。监测期数据导出包含所有设备。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::startAiExport,
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("AI 导出") }
            Text(
                "即贴即用的报告附录：包含频次、RSSI 信号分级、重点关注与追踪 ID。不会重复列出研判清单。单设备 AI 导出可在设备详情中进行。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("监测期数据导出") {
            val sitKind by vm.sitExportKind.collectAsStateWithLifecycle()
            val sitRadios by vm.sitExportRadios.collectAsStateWithLifecycle()
            ExportFormatBlock(
                kind = sitKind,
                radios = sitRadios,
                exporting = exporting,
                onKind = vm::setSitExportKind,
                onRadios = vm::setSitExportRadios,
                onShare = vm::startSitExport,
                onSave = onSaveSitToStorage,
                hint = "每行记录此监测期（或过去 15 分钟）内侦听到的唯一无线电设备。CSV / JSONL 包含匹配特征与重点关注类别。不同于轮换日志。GPX / KML 包含本机的移动轨迹及侦听点。Fieldwatch 不会联网上传。隐私模式不会混淆此导出文件。",
            )
            }

            SectionCard("监测期对比") {
                Text(
                    compareThisCaption(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val thisSaved = SitDiff.thisSavedId(state.sit.open, state.sit.selectedId)
                val choices = SitDiff.secondSitChoices(state.sit.closed, thisSaved)
                if (choices.isEmpty()) {
                    Text(
                        "保存第二个监测期以进行对比。点击“开启监测期”，随后“结束监测期”。内存中过去 15 分钟的数据可用作当前监测期。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "对比的第二个监测期",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    choices.forEach { row ->
                        val dur = Sit.fmtDuration(row.durationMs())
                        SitChoiceRow(
                            selected = state.sit.compareId == row.id,
                            enabled = !exporting,
                            title = row.name,
                            subtitle = "${Sit.defaultName(row.startAt)} · $dur · ${row.radioCount} 台设备",
                            onSelect = { vm.selectCompareSit(row.id) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FieldwatchActionButton(
                        onClick = vm::startSitCompare,
                        enabled = !exporting && state.sit.compareId != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("对比报告 (文本)") }
                    FieldwatchActionButton(
                        onClick = vm::startSitComparePdf,
                        enabled = !exporting && state.sit.compareId != null,
                        modifier = Modifier.weight(1f),
                    ) { Text("对比报告 (PDF)") }
                }
                Text(
                    "报告内容相同，提供两种格式。仅比对出现情况 —— 仅在当前监测期、仅在第二监测期、两者均有。包含类型与 MAC。重点关注与已命名设备会有标记。并非无线电发射源定位。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FieldwatchActionButton(
                    onClick = vm::startSitCompareAiExport,
                    enabled = !exporting && state.sit.compareId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("AI 导出") }
                Text(
                    "即贴即用的研判附录：重叠率、独有重点关注/已命名设备、对比可剔除的背景设备。不会重复列出完整对比清单。监测期研判 AI 导出仅限当前时间窗口。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SectionCard("特征库维护") {
            FieldwatchActionButton(
                onClick = onSignatureCandidates,
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("提取特征候选") }
            Text(
                "从日志中筛选出具有相同唯一标识的未匹配设备 —— 而非所有未知设备。由您审核确认，保存前不会添加任何内容。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("日志导出") {
            Text(
                "本次运行记录 ${state.logLines} 行  ·  磁盘占用 ${vm.logBytes() / 1024} KB" +
                    if (settings.loggingEnabled) "" else "  ·  日志记录已关闭",
                style = MaterialTheme.typography.bodySmall,
            )
            val logKind by vm.logExportKind.collectAsStateWithLifecycle()
            val logRadios by vm.logExportRadios.collectAsStateWithLifecycle()
            ExportFormatBlock(
                kind = logKind,
                radios = logRadios,
                exporting = exporting,
                onKind = vm::setLogExportKind,
                onRadios = vm::setLogExportRadios,
                onShare = vm::startExport,
                onSave = onSaveToStorage,
                hint = "轮换文件采用 JSON Lines 格式。CSV 适合电子表格查看。GPX (GPS Exchange)、KML (Google Earth) 与 WiGLE CSV (wigle.net) 为侦听点数据：记录本机侦听到设备时本机所处位置，而非设备本身定位。需开启 GPS 定位标记与日志记录。分享调用 Android 系统分享面板 —— Fieldwatch 不会联网上传。",
            )
            FieldwatchActionButton(
                onClick = { confirmClear = true },
                enabled = !exporting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("重置 / 清空日志")
            }
            if (confirmClear) {
                AlertDialog(
                    onDismissRequest = { confirmClear = false },
                    title = { Text("清空日志？") },
                    text = {
                        Text("这将删除手机上存储的所有轮换 CSV/JSON 文件。此操作无法撤销。实时扫描将创建全新的空日志文件。")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmClear = false
                            vm.clearLogs()
                        }) { Text("清空日志") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmClear = false }) { Text("取消") }
                    },
                )
            }
            }
        }
    }
    if (startSit) {
        AlertDialog(
            onDismissRequest = { startSit = false },
            title = { Text("开启监测期") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FieldwatchOutlinedField(
                        value = sitNameDraft,
                        onValueChange = { sitNameDraft = it.take(Sit.NAME_MAX) },
                        label = "监测期名称",
                    )
                    Text(
                        "在您手动结束前，态势研判报告与 AI 导出将以此监测期的时间窗口为准。实时监控列表不受影响。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Sit.dropWarning(state.sit.closed)?.let { warn ->
                        Text(
                            warn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    startSit = false
                    vm.startSit(sitNameDraft)
                }) { Text("开启") }
            },
            dismissButton = {
                TextButton(onClick = { startSit = false }) { Text("取消") }
            },
        )
    }
    val renaming = renameSitId
    if (renaming != null) {
        AlertDialog(
            onDismissRequest = { renameSitId = null },
            title = { Text("重命名监测期") },
            text = {
                FieldwatchOutlinedField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(Sit.NAME_MAX) },
                    label = "监测期名称",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    renameSitId = null
                    vm.renameSit(renaming, renameDraft)
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameSitId = null }) { Text("取消") }
            },
        )
    }
    val deleting = deleteSitId
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleteSitId = null },
            title = { Text("删除此监测期？") },
            text = { Text("从本机删除此保存的监测期记录。原始日志不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteSitId = null
                    vm.deleteSit(deleting)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteSitId = null }) { Text("取消") }
            },
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { Text("删除所有历史监测期？") },
            text = { Text("从本机删除所有已保存的历史监测期。进行中的监测期不会被删除。原始日志不受影响。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAll = false
                    vm.deleteAllSits()
                }) { Text("全部删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SitChoiceRow(
    selected: Boolean,
    enabled: Boolean,
    title: String,
    subtitle: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
        )
        Column(Modifier.padding(start = 8.dp).fillMaxWidth()) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected && enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AdvertisedTrackSwatch() {
    Canvas(Modifier.width(28.dp).height(10.dp)) {
        val dash = 3.dp.toPx()
        val gap = 4.5.dp.toPx()
        drawLine(
            Color.White,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, gap), 0f),
        )
    }
}

@Composable
private fun AdvertisedRingSwatch() {
    Canvas(Modifier.size(12.dp)) {
        drawCircle(
            Color.White,
            radius = size.minDimension / 2f - 1.dp.toPx(),
            style = Stroke(width = 1.6.dp.toPx()),
        )
    }
}

@Composable
private fun PilotSwatch() {
    val painter = rememberVectorPainter(Icons.Outlined.Person)
    Canvas(Modifier.size(18.dp)) {
        val radius = size.minDimension / 2f
        val disc = radius * 0.86f
        drawCircle(Color.White, radius = radius)
        drawCircle(Color(0xFFF4F7FB), radius = disc)
        drawCircle(Color(0xFF3D4A55), radius = disc, style = Stroke(width = 1.2.dp.toPx()))
        val icon = disc * 1.35f
        translate((size.width - icon) / 2f, (size.height - icon) / 2f) {
            with(painter) {
                draw(Size(icon, icon), colorFilter = ColorFilter.tint(Color(0xFF3D4A55)))
            }
        }
    }
}

@Composable
private fun PathRadioRow(
    index: Int,
    dot: SitPathPlot.Dot,
    demoMode: Boolean,
    onOpen: () -> Unit,
) {
    val mac = MacUtil.screenMac(dot.mac, demoMode)
    val named = dot.label.isNotBlank() && !dot.label.equals(mac, ignoreCase = true)
    val fleets = dot.fleetNames.joinToString(" · ")
    val note = dot.observerNotes.trim()
    val accent = (if (dot.accentArgb != 0) Color(dot.accentArgb) else MaterialTheme.colorScheme.onSurfaceVariant)
        .nightIf(LocalNightMode.current)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$index",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(22.dp),
        )
        RadioClassBadge(dot.classKind, accent, compact = true)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            if (named) {
                Text(
                    dot.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (mac.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioKindMark(dot.kind, size = 13.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        mac,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (fleets.isNotEmpty()) {
                Text(
                    fleets,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (note.isNotEmpty()) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = Cyan.nightIf(LocalNightMode.current),
                )
            }
        }
    }
}

private fun compareThisCaption(state: FieldwatchUi): String {
    val open = state.sit.open
    if (open != null) {
        return "当前监测期：${open.name} — 指定时间窗口（最多 ${Sit.RADIO_CAP} 台设备）。与研判报告范围相同。"
    }
    val selected = state.sit.closed.firstOrNull { it.id == state.sit.selectedId }
    if (selected != null) {
        return "当前监测期：${selected.name} — 指定时间窗口（最多 ${Sit.RADIO_CAP} 台设备）。与研判报告范围相同。"
    }
    return "当前监测期：内存中过去 15 分钟（约 400 台设备）。与研判报告范围相同。"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExportFormatBlock(
    kind: LogExportKind,
    radios: LogExportRadios,
    exporting: Boolean,
    onKind: (LogExportKind) -> Unit,
    onRadios: (LogExportRadios) -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    hint: String,
) {
    var openFormat by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = openFormat,
        onExpandedChange = { openFormat = it },
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        FieldwatchDropdownField("导出格式", kind.label, openFormat)
        ExposedDropdownMenu(openFormat, { openFormat = false }) {
            LogExportKind.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onKind(item)
                        openFormat = false
                    },
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LogExportRadios.entries.forEach { item ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = radios == item,
                        onClick = { onRadios(item) },
                        role = Role.RadioButton,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = radios == item,
                    onClick = { onRadios(item) },
                    enabled = !exporting,
                )
                Text(item.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    FieldwatchActionButton(
        onClick = onShare,
        enabled = !exporting,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("分享") }
    FieldwatchActionButton(
        onClick = onSave,
        enabled = !exporting,
        modifier = Modifier.fillMaxWidth(),
    ) { Text("保存至存储卡 / 本地存储…") }
    Text(
        hint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun sitReportCaption(state: FieldwatchUi): String {
    val open = state.sit.open
    if (open != null) {
        return "当前监测期 (${open.name}) — 与移动轨迹时间窗口相同。开启定位标记且移动时可进行伴随研判。非司法鉴定结论。"
    }
    val selected = state.sit.closed.firstOrNull { it.id == state.sit.selectedId }
    if (selected != null) {
        return "监测期：${selected.name} — 与移动轨迹时间窗口相同。开启定位标记且移动时可进行伴随研判。非司法鉴定结论。"
    }
    return "内存中过去 15 分钟 — 与移动轨迹时间窗口相同。提供两种格式。开启定位标记且移动时可进行伴随研判。非司法鉴定结论。"
}
