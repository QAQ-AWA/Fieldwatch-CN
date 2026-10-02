package app.fieldwatch.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import app.fieldwatch.ui.component.FieldwatchActionButton
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.Scaffold
import app.fieldwatch.ui.component.FieldwatchSlider
import androidx.compose.material3.Surface
import app.fieldwatch.ui.component.FieldwatchSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldwatch.domain.FilterLogic
import app.fieldwatch.domain.FilterPreset
import app.fieldwatch.domain.Fleet
import app.fieldwatch.domain.SignatureClass
import app.fieldwatch.ui.ClassGlyphs
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.component.SectionCard
import app.fieldwatch.ui.component.FieldwatchFilterChip
import app.fieldwatch.ui.component.spectreSectionFill
import app.fieldwatch.ui.component.spectreTileEdge
import app.fieldwatch.ui.component.spectreTileFill
import app.fieldwatch.ui.theme.LocalNightMode
import app.fieldwatch.ui.theme.PhosphorActive
import app.fieldwatch.ui.theme.nightIf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersScreen(state: FieldwatchUi, vm: FieldwatchViewModel) {
    var presetName by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<FilterPreset?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val filter = state.filter
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = { NestedTopBar("过滤规则") },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("预设模板") {
            Text(
                "轻触以应用完整过滤规则。长按标签可将其删除。" +
                    "已内置常用预设，在下方输入名称并保存可添加自定义预设（如：监控摄像、广场 −80dBm 等）。" +
                    "已删除的内置预设可在“设置 → 恢复默认特征库与预设”中恢复。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.presets.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { preset ->
                                PresetChip(
                                    name = preset.name,
                                    selected = preset.filter == filter,
                                    onApply = { vm.applyPreset(preset) },
                                    onLongPress = { pendingDelete = preset },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldwatchOutlinedField(
                    presetName,
                    { presetName = it },
                    "将当前规则另存为…",
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    if (presetName.isNotBlank()) {
                        vm.savePreset(presetName.trim())
                        presetName = ""
                    }
                }) { Text("保存") }
            }
            }

            SectionCard("无线电类型") {
            Text(
                if (filter.movingWithYou) {
                    "“随行移动”仅适用于 BLE。在关闭该开关前，“全部”和“仅限 Wi-Fi”将保持禁用。"
                } else {
                    "包含开关。全部开启可同时查看 Wi-Fi 和 BLE（单个射频设备不会同时属于两者）。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldwatchFilterChip(
                    selected = !filter.movingWithYou && filter.showWifi && filter.showBle,
                    onClick = { vm.updateFilter { it.copy(showWifi = true, showBle = true) } },
                    enabled = !filter.movingWithYou,
                    label = { Text("全部") },
                )
                FieldwatchFilterChip(
                    selected = !filter.movingWithYou && filter.showWifi && !filter.showBle,
                    onClick = { vm.updateFilter { it.copy(showWifi = true, showBle = false) } },
                    enabled = !filter.movingWithYou,
                    label = { Text("仅限 Wi-Fi") },
                )
                FieldwatchFilterChip(
                    selected = filter.movingWithYou || (filter.showBle && !filter.showWifi),
                    onClick = { vm.updateFilter { it.copy(showWifi = false, showBle = true) } },
                    label = { Text("仅限 BLE") },
                )
            }
            }

            SectionCard("随行移动") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("随行移动", Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.movingWithYou,
                    { on ->
                        vm.updateFilter { current ->
                            if (!on) current.copy(movingWithYou = false)
                            else {
                                // Follow test is BLE. Leftover Trackers / Show only hides
                                // unmatched rows; AirTags rotate, so Live looks empty.
                                val hiding = current.useClassFilter && current.excludeClasses
                                current.copy(
                                    movingWithYou = true,
                                    showWifi = false,
                                    showBle = true,
                                    namedOnly = false,
                                    customNamesOnly = false,
                                    watchedOnly = false,
                                    useClassFilter = hiding,
                                    excludeClasses = hiding,
                                    classes = if (hiding) current.classes else emptySet(),
                                    includeSignatures = false,
                                )
                            }
                        }
                    },
                )
            }
            Text(
                when {
                    !state.settings.tagLocation ->
                        "请先开启“设置 → 使用 GPS 标记检出位置”，然后步行或驾车。" +
                            "仅筛选沿移动路径持续跟随的高信号强度 BLE 广播设备。" +
                            "Wi-Fi 接入点将被屏蔽——因为驾车驶过的高发射功率 AP 可能会误连成轨迹。" +
                            "开启该开关将开始 BLE 尾随测试（会自动清除仅特征/仅显示/仅命名设备/仅关注项等过滤）。" +
                            "您也可以直接轻触顶部的“随行移动”预设。"
                    state.operatorSpanM < 45.0 ->
                        "当前 GPS 路径已累计 ${state.operatorSpanM.toInt()} 米。请继续移动（需移动约 50 米以上）。" +
                            "若在行进过程中数值仍为 0，说明系统定位未提供实时定位数据" +
                            "（请将定位模式设为高精度，仅获取“上次已知位置”不足以支撑计算）。" +
                            "另外第二台 iPhone 通常无法匹配，因为 BLE MAC 地址轮换后会被视作新设备。" +
                            when {
                                filter.customNamesOnly ->
                                    " “仅限命名设备”当前已开启——未标注名称的设备将被隐藏。"
                                filter.watchedOnly ->
                                    " “仅限关注项”当前已开启——未关注的设备将被隐藏。"
                                filter.namedOnly || filter.namedOnlyImplied() ->
                                    " “仅限特征 / 仅显示此类”当前已开启——不匹配的设备将被隐藏。"
                                else -> ""
                            }
                    filter.customNamesOnly || filter.watchedOnly || filter.namedOnly || filter.namedOnlyImplied() ->
                        "GPS 路径已累计 ${state.operatorSpanM.toInt()} 米。“仅限特征”、“按类别仅显示”、" +
                            "“仅限命名设备”或“仅限关注项”处于开启状态，因此仅有符合这些条件的设备才会被判定为伴随移动。" +
                            "轻触“随行移动”预设可测试 BLE 伴随。放置在随身包内或车内的防丢标签将会匹配。Wi-Fi 接入点保持隐藏。"
                    else ->
                        "GPS 路径已累计 ${state.operatorSpanM.toInt()} 米。已筛选出沿此路径持续出现且信号强度相对稳定的高功率 BLE 设备" +
                            "——排除了仅在您到达目的地后才出现的设备。放置在背包或车内的防丢标签将会被匹配。Wi-Fi 接入点保持隐藏" +
                            "（因其射频覆盖范围大容易误判为同行）。手机轮换的 BLE 隐私 MAC 无法自动串联为同一追踪者。" +
                            "“实时 → 重新开始”可清空路径与航迹以便重新测试。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("新检出设备") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (state.arrivalsLearning) "仅限新检出设备  ·  学习基准中" else "仅限新检出设备",
                    Modifier.weight(1f),
                )
                FieldwatchSwitch(
                    filter.arrivalsOnly,
                    { on -> vm.updateFilter { it.copy(arrivalsOnly = on) } },
                )
            }
            Text(
                if (filter.arrivalsOnly) {
                    "“标记已读”与“重置已读”位于“实时”界面顶部标签上方。" +
                        when {
                            state.arrivalsLearning ->
                                "正在将当前静止的 Wi-Fi 信号学习纳入已读基准。"
                            state.hiddenKnown > 0 ->
                                "已隐藏 ${state.hiddenKnown} 个已检出的旧设备。"
                            else ->
                                "已检出旧设备数为 0。"
                        }
                } else {
                    "隐藏已经在此处存在的设备，仅在“实时”列表中显示新检出设备。" +
                        "开启后，“实时”界面上方会显示“标记已读”与“重置已读”按钮。" +
                        "“短暂停留”设置决定了新设备在最后一个数据包后保留显示的时长。" +
                        "使用随机隐私地址的 BLE 设备每次变换均会视为新设备。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("保留对象") {
            val namedImplied = filter.namedOnlyImplied()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "仅限已知特征 (隐藏不匹配设备)",
                    Modifier.weight(1f),
                    color = if (namedImplied) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                FieldwatchSwitch(
                    checked = filter.namedOnly || namedImplied,
                    onCheckedChange = { on ->
                        if (!namedImplied) vm.updateFilter { it.copy(namedOnly = on) }
                    },
                    enabled = !namedImplied,
                )
            }
            if (namedImplied) {
                Text(
                    "“仅显示此类”已自动隐藏不匹配设备。需关闭“仅显示此类”（类别或指定特征）后方可使用此开关。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("仅限关注项", Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.watchedOnly,
                    { on -> vm.updateFilter { it.copy(watchedOnly = on) } },
                )
            }
            Text(
                "仅显示匹配已标星关注的特征、或已开启报警的命名设备。" +
                    "“排除此类”仍有效（如：开启“仅限关注项”但勾选“排除监控监测”，则已关注的摄像头仍会被隐藏）。" +
                    "仅打标签未开报警的设备归于“仅限命名设备”。在“特征库”中可添加关注星标；在详情页可开启报警。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("仅限自定义命名设备", Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.customNamesOnly,
                    { on -> vm.updateFilter { it.copy(customNamesOnly = on) } },
                )
            }
            Text(
                "仅显示已赋予自定义备注名称的设备（报警开关无需开启）。前往“设置 → 自定义命名设备”管理。" +
                    "使用随机隐私 MAC 的设备在地址轮换后无法继续跟随匹配。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("隐藏 Fast Pair 账号密钥广播", Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.hideFastPairAccountKey,
                    { on -> vm.updateFilter { it.copy(hideFastPairAccountKey = on) } },
                )
            }
            Text(
                "过滤嘈杂环境噪音：过滤已完成配对的 Fast Pair 广播芯片（未匹配其他特征）。" +
                    "仍保留处于配对模式中的设备（轻触可配对的模型 ID）。如果在特征中手动排除 Fast Pair，则配对模式也会一并隐藏。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("特征类别") {
            Text(
                "仅对“实时”视图生效——特征仍会正常标注、记录并在触发时蜂鸣提示。" +
                    "摄像头、无人机、寻物防丢标签等均可通过以下标签筛选——先选“仅显示此类”，若满意可在上方“另存为预设”。" +
                    "若选了“仅显示此类”但未勾选任何类别，实时列表将保持不变。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldwatchFilterChip(
                    selected = filter.useClassFilter && !filter.excludeClasses,
                    onClick = {
                        vm.updateFilter {
                            val on = !(it.useClassFilter && !it.excludeClasses)
                            it.copy(useClassFilter = on, excludeClasses = false)
                        }
                    },
                    label = { Text("仅显示此类") },
                )
                FieldwatchFilterChip(
                    selected = filter.useClassFilter && filter.excludeClasses,
                    onClick = {
                        vm.updateFilter {
                            val on = !(it.useClassFilter && it.excludeClasses)
                            it.copy(useClassFilter = on, excludeClasses = on)
                        }
                    },
                    label = { Text("排除此类") },
                )
            }
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SignatureClass.visible.sortedBy { it.label().lowercase() }.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { kind ->
                                val on = kind in filter.classes
                                FieldwatchFilterChip(
                                    selected = on,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(max = 32.dp),
                                    onClick = {
                                        vm.updateFilter { current ->
                                            val next = current.classes.toMutableSet()
                                            if (on) next.remove(kind) else next.add(kind)
                                            current.copy(classes = next)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            ClassGlyphs.of(kind),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    },
                                    label = {
                                        Text(
                                            kind.label(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                )
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            }

            SectionCard("指定特征库项") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("仅显示选中的特征", Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.includeSignatures,
                    { on -> vm.updateFilter { it.copy(includeSignatures = on) } },
                )
            }
            if (filter.includeSignatures) {
                SignaturePickList(
                    fleets = state.fleets,
                    selected = filter.includeFleetIds,
                    help = "轻触类别可展开其包含的具体特征。开启后，只有匹配下方所选特征的设备才会保留在“实时”列表中。" +
                        "列表全空时不对实时列表做额外限制。开关关闭后再开启仍会保留当前勾选。",
                    onToggle = { id, checked ->
                        vm.updateFilter { current ->
                            val next = current.includeFleetIds.toMutableSet()
                            if (checked) next.add(id) else next.remove(id)
                            current.copy(includeFleetIds = next)
                        }
                    },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("排除选中的特征", Modifier.weight(1f))
                FieldwatchSwitch(
                    filter.excludeSignatures,
                    { on -> vm.updateFilter { it.copy(excludeSignatures = on) } },
                )
            }
            if (filter.excludeSignatures) {
                SignaturePickList(
                    fleets = state.fleets,
                    selected = filter.fleetIds,
                    help = "轻触类别可展开其包含的具体特征。匹配下方勾选特征的设备将从“实时”列表中过滤隐藏。" +
                        "开关关闭后再开启仍会保留当前勾选。",
                    onToggle = { id, checked ->
                        vm.updateFilter { current ->
                            val next = current.fleetIds.toMutableSet()
                            if (checked) next.add(id) else next.remove(id)
                            current.copy(fleetIds = next)
                        }
                    },
                )
            }
            }

            SectionCard("精细过滤") {
            var rssiDrag by remember { mutableIntStateOf(filter.rssiMin) }
            var rssiDragging by remember { mutableStateOf(false) }
            LaunchedEffect(filter.rssiMin) {
                if (!rssiDragging) rssiDrag = filter.rssiMin
            }
            Text("最低信号强度 (RSSI)  $rssiDrag dBm", style = MaterialTheme.typography.labelLarge)
            FieldwatchSlider(
                value = rssiDrag.toFloat(),
                onValueChange = { v ->
                    rssiDragging = true
                    rssiDrag = v.toInt()
                },
                onValueChangeFinished = {
                    vm.updateFilter { it.copy(rssiMin = rssiDrag) }
                    rssiDragging = false
                },
                valueRange = -100f..-30f,
            )

            FieldwatchOutlinedField(
                filter.nameQuery,
                { value -> vm.updateFilter { it.copy(nameQuery = value) } },
                "名称 / MAC 包含",
            )
            FieldwatchOutlinedField(
                filter.ouiQuery,
                { value -> vm.updateFilter { it.copy(ouiQuery = value) } },
                "OUI / 厂商包含",
            )

            Text("复合过滤逻辑", style = MaterialTheme.typography.labelLarge)
            Text(
                "“与/或”逻辑适用于名称、OUI、信号强度 (RSSI) 和类别包含规则；不适用于无线电开关、仅限命名设备、仅限关注项、隐藏 Fast Pair 账号密钥或排除列表。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FieldwatchFilterChip(
                    selected = filter.logic == FilterLogic.AND,
                    onClick = { vm.updateFilter { it.copy(logic = FilterLogic.AND) } },
                    label = { Text("与 (AND)") },
                )
                FieldwatchFilterChip(
                    selected = filter.logic == FilterLogic.OR,
                    onClick = { vm.updateFilter { it.copy(logic = FilterLogic.OR) } },
                    label = { Text("或 (OR)") },
                )
            }

            FieldwatchActionButton(onClick = { confirmReset = true }) {
                Text("重置过滤规则")
            }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("重置过滤规则？") },
            text = {
                Text(
                    "将清空此标签页中的所有开关与选中项（无线电、类别、指定特征、RSSI、名称/OUI）。" +
                        "您已保存的自定义预设将继续保留。“实时”视图将恢复显示全部未过滤数据。此操作不可撤销。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReset = false
                        vm.updateFilter { app.fieldwatch.domain.FilterState() }
                    },
                ) { Text("重置") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("取消") }
            },
        )
    }
    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除预设？") },
            text = {
                Text(
                    if (preset.isBuiltIn()) {
                        "从列表中移除内置预设“${preset.name}”？后续特征库更新不会恢复此项。可在“设置 → 恢复默认特征库与预设”中恢复所有内置预设。“实时”界面中的过滤规则不会立即改变，直到您应用其他预设或重置过滤规则。"
                    } else {
                        "删除自定义预设“${preset.name}”？此操作不可撤销。“实时”界面中的过滤规则不会立即改变，直到您应用其他预设或重置过滤规则。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deletePreset(preset.id)
                    pendingDelete = null
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun SignaturePickList(
    fleets: List<Fleet>,
    selected: Set<String>,
    help: String,
    onToggle: (id: String, checked: Boolean) -> Unit,
) {
    val groups = remember(fleets) {
        fleets.groupBy { it.kind.folded() }
            .toList()
            .sortedBy { it.first.label().lowercase() }
            .map { (kind, rows) -> kind to rows.sortedBy { it.name.lowercase() } }
    }
    var open by remember {
        mutableStateOf(
            groups.filter { (_, rows) -> rows.any { it.id in selected } }
                .map { it.first.name }
                .toSet(),
        )
    }
    Column(
        modifier = Modifier.padding(start = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            help,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        groups.forEach { (kind, rows) ->
            val classId = kind.name
            val expanded = classId in open
            val picked = rows.count { it.id in selected }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        open = if (expanded) open - classId else open + classId
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    ClassGlyphs.of(kind),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    kind.label(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(if (expanded) "▾  " else "▸  ")
                        if (picked > 0) append("$picked/")
                        append(rows.size)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (picked > 0) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            if (expanded) {
                rows.forEach { fleet ->
                    val on = fleet.id in selected
                    Row(
                        modifier = Modifier.padding(start = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(fleet.name, Modifier.weight(1f))
                        FieldwatchSwitch(on, { checked -> onToggle(fleet.id, checked) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetChip(
    name: String,
    selected: Boolean = false,
    onApply: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = FilterChipDefaults.shape,
        color = if (selected) spectreSectionFill() else spectreTileFill(),
        border = BorderStroke(
            1.dp,
            if (selected) PhosphorActive.nightIf(LocalNightMode.current) else spectreTileEdge(),
        ),
        modifier = modifier
            .heightIn(max = 32.dp)
            .combinedClickable(
                onClick = onApply,
                onLongClick = onLongPress,
            ),
    ) {
        Text(
            name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
