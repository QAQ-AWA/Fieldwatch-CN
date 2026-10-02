package app.fieldwatch.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Check
import app.fieldwatch.ui.component.DecodeGlyph
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import app.fieldwatch.ui.component.FieldwatchFilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import app.fieldwatch.ui.component.FieldwatchActionButton
import app.fieldwatch.ui.component.FieldwatchDropdownField
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import app.fieldwatch.ui.component.FieldwatchSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fieldwatch.domain.Fleet
import app.fieldwatch.domain.MatchRule
import app.fieldwatch.domain.Palette
import app.fieldwatch.ui.theme.LocalNightMode
import app.fieldwatch.ui.theme.nightIf
import app.fieldwatch.domain.RadioKind
import app.fieldwatch.domain.RuleKind
import app.fieldwatch.domain.SignatureClass
import app.fieldwatch.domain.SignatureListSort
import app.fieldwatch.domain.groupedByClass
import app.fieldwatch.domain.sortedForCatalog
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.RadioClassBadge
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.component.SectionCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetsScreen(
    state: FieldwatchUi,
    vm: FieldwatchViewModel,
    onCandidateDraftClosed: () -> Unit = {},
) {
    val draft = state.draftFleet
    if (draft != null) {
        val isNew = state.fleets.none { it.id == draft.id }
        var working by remember(draft.id) { mutableStateOf(draft) }
        var editingDecode by remember(draft.id) { mutableStateOf(false) }
        if (editingDecode) {
            val preview = state.devices.firstOrNull { working.id in it.fleetIds }
                ?: state.selected?.takeIf { working.id in it.fleetIds }
            DecodeFieldsScreen(
                fleet = working,
                previewDevice = preview,
                onSave = { decode ->
                    val next = working.copy(decode = decode)
                    working = next
                    vm.saveFleetKeepDraft(next)
                    editingDecode = false
                },
                onBack = { editingDecode = false },
            )
            return
        }
        FleetEditor(
            working,
            isNew = isNew,
            onSave = { fleet ->
                val bounce = vm.takeDraftFromCandidates()
                vm.upsertFleet(fleet) {
                    if (bounce) {
                        vm.startSignatureCandidates()
                        onCandidateDraftClosed()
                    }
                }
            },
            onCancel = {
                val bounce = vm.takeDraftFromCandidates()
                vm.cancelDraft()
                if (bounce) onCandidateDraftClosed()
            },
            onDelete = if (isNew) null else ({ vm.deleteFleet(draft.id) }),
            onOpenDecode = { current ->
                working = current
                editingDecode = true
            },
        )
        return
    }
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = { NestedTopBar("特征库 (${state.fleets.size})") },
        floatingActionButton = {
            FloatingActionButton(onClick = vm::beginNewFleet) {
                Icon(Icons.Outlined.Add, "新建特征")
            }
        },
    ) { pad ->
        val sort = state.settings.signatureListSort
        val openClasses by vm.catalogOpenClasses.collectAsStateWithLifecycle()
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Text(
                    if (sort == SignatureListSort.CLASS) {
                        "点击类别以展开其特征。书签 = 发现时提示音。如需隐藏请前往“过滤器”，而非此处。"
                    } else {
                        "点击以编辑。书签 = 发现该类别时提示音。如需隐藏请前往“过滤器”，而非此处。"
                    },
                    style = compactLine(12.sp, 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    FieldwatchFilterChip(
                        selected = sort == SignatureListSort.NAME,
                        onClick = { vm.setSignatureListSort(SignatureListSort.NAME) },
                        label = { Text("按名称 A–Z") },
                    )
                    FieldwatchFilterChip(
                        selected = sort == SignatureListSort.CLASS,
                        onClick = { vm.setSignatureListSort(SignatureListSort.CLASS) },
                        label = { Text("按类别 A–Z") },
                    )
                }
            }
            if (sort == SignatureListSort.CLASS) {
                state.fleets.groupedByClass().forEach { (kind, rows) ->
                    val classId = kind.name
                    val expanded = classId in openClasses
                    item(key = "class-$classId") {
                        SignatureClassHeader(
                            kind = kind,
                            count = rows.size,
                            colorIndex = rows.firstOrNull()?.colorIndex ?: 0,
                            expanded = expanded,
                            onToggle = { vm.toggleCatalogClass(classId) },
                        )
                    }
                    if (expanded) {
                        items(rows, key = { it.id }) { fleet ->
                            Box(Modifier.padding(start = 16.dp)) {
                                SignatureRow(fleet, state, vm)
                            }
                        }
                    }
                }
            } else {
                items(state.fleets.sortedForCatalog(sort), key = { it.id }) { fleet ->
                    SignatureRow(fleet, state, vm)
                }
            }
        }
    }
}

@Composable
private fun SignatureClassHeader(
    kind: SignatureClass,
    count: Int,
    colorIndex: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val accent = Color(Palette.color(colorIndex)).nightIf(LocalNightMode.current)
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioClassBadge(classKind = kind, accent = accent)
            Spacer(Modifier.width(10.dp))
            Text(
                kind.label(),
                style = compactLine(16.sp, 18.sp, FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (expanded) "▾  $count" else "▸  $count",
                style = compactLine(14.sp, 16.sp, FontWeight.Bold).copy(fontFamily = FontFamily.Monospace),
                color = accent,
            )
        }
    }
}

@Composable
private fun SignatureRow(fleet: Fleet, state: FieldwatchUi, vm: FieldwatchViewModel) {
    val color = Color(Palette.color(fleet.colorIndex)).nightIf(LocalNightMode.current)
    val liveHits = state.devices.count { fleet.id in it.fleetIds && !it.gone }
    Surface(
        onClick = { vm.editFleet(fleet) },
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioClassBadge(
                classKind = fleet.kind,
                accent = color,
            )
            Spacer(Modifier.width(8.dp))
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        fleet.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = compactLine(16.sp, 18.sp, FontWeight.SemiBold),
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (fleet.decode != null) {
                        DecodeGlyph(
                            tint = color,
                            size = 14.dp,
                        )
                    }
                }
                Text(
                    "${fleet.kind.label()} · ${fleet.rules.size} 条规则 · $liveHits 个在线 · ${if (fleet.matchAny) "任一匹配 (或)" else "全部匹配 (与)"}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = compactLine(11.sp, 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { vm.toggleWatchFleet(fleet) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    if (vm.isFleetWatched(fleet.id)) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    "侦听到此特征时发出提示音",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetEditor(
    initial: Fleet,
    isNew: Boolean = false,
    onSave: (Fleet) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onOpenDecode: (Fleet) -> Unit = {},
) {
    var fleet by remember(initial.id) { mutableStateOf(initial) }
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(initial.decode) {
        fleet = fleet.copy(decode = initial.decode)
    }
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = {
            NestedTopBar(
                title = if (isNew) "新建特征" else "编辑特征",
                navigationIcon = { TextButton(onClick = onCancel) { Text("取消") } },
                actions = { TextButton(onClick = { onSave(fleet) }) { Text("保存") } },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("基本信息") {
            FieldwatchOutlinedField(fleet.name, { fleet = fleet.copy(name = it) }, "特征名称")
            FieldwatchOutlinedField(
                fleet.notes,
                { fleet = fleet.copy(notes = it) },
                "特征备注",
                supportingText = "显示在匹配设备的详情页、分享文案以及 AI 导出中。不同于重点关注 —— 实时界面无“!”标记，亦无琥珀色高亮卡片。",
                singleLine = false,
                minLines = 2,
            )
            FieldwatchOutlinedField(
                fleet.attentionNote,
                { fleet = fleet.copy(attentionNote = it) },
                "重点关注提示",
                supportingText = "可选。填入内容后，匹配设备将在实时监控中显示“!”标记、在设备详情中显示琥珀色警告卡片并在研判报告中单独罗列。与上述普通备注相互独立。",
                singleLine = false,
                minLines = 3,
            )
            }

            SectionCard("匹配逻辑") {
            var classMenu by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(classMenu, { classMenu = it }) {
                FieldwatchDropdownField("所属类别", fleet.kind.label(), classMenu)
                ExposedDropdownMenu(classMenu, { classMenu = false }) {
                    SignatureClass.visible.sortedBy { it.label().lowercase() }.forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(kind.label()) },
                            onClick = {
                                fleet = fleet.copy(kind = kind)
                                classMenu = false
                            },
                        )
                    }
                }
            }
            Text(
                "在“过滤器”中可选择“仅显示”或“隐藏”。各类特征预设（防丢标签、摄像头等）即为这些标签组 —— 如需预设可点击“将当前另存为…”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("满足任一规则即匹配 (OR)", Modifier.weight(1f))
                FieldwatchSwitch(fleet.matchAny, { fleet = fleet.copy(matchAny = it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("按 OUI 聚类", Modifier.weight(1f))
                FieldwatchSwitch(fleet.clusterByOui, { fleet = fleet.copy(clusterByOui = it) })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("连续递增 MAC 地址", Modifier.weight(1f))
                FieldwatchSwitch(fleet.sequentialMac, { fleet = fleet.copy(sequentialMac = it) })
            }
            FieldwatchOutlinedField(
                fleet.minPeers.toString(),
                { fleet = fleet.copy(minPeers = it.toIntOrNull() ?: 0) },
                "最少对等设备数 (0 = 关闭)",
            )
            FieldwatchOutlinedField(
                fleet.peerWindowSec.toString(),
                { fleet = fleet.copy(peerWindowSec = it.toIntOrNull() ?: 60) },
                "对等设备观察窗口 (秒)",
            )
            }

            SectionCard("标识颜色") {
            ColorPicker(fleet.colorIndex) { fleet = fleet.copy(colorIndex = it) }
            Text(
                "默认颜色按类别划分（红:渗透测试，琥珀:摄像头/车牌识别，紫:手机/标签，青:穿戴设备，绿:网状网络，橙:音频/眼镜，蓝绿:车载/载具）。可为任意特征单独修改。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("规则列表") {
            Text(
                "每条规则均有独立开关。关闭时保留规则但不参与匹配。可用于临时屏蔽特征中过于嘈杂的 OUI 或名称，无需将其删除。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            fleet.rules.forEachIndexed { index, rule ->
                RuleEditor(
                    rule = rule,
                    onChange = { next ->
                        val rules = fleet.rules.toMutableList()
                        rules[index] = next
                        fleet = fleet.copy(rules = rules)
                    },
                    onDelete = {
                        fleet = fleet.copy(rules = fleet.rules.filterIndexed { i, _ -> i != index })
                    },
                )
            }
            FieldwatchActionButton(
                onClick = {
                    fleet = fleet.copy(rules = fleet.rules + MatchRule(RuleKind.OUI, text = ""))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("添加规则") }
            }

            if (fleet.canHaveBleDecode()) {
                val decodeCount = fleet.decode?.fields?.size ?: 0
                SectionCard("载荷解码规则") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDecode(fleet) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (decodeCount == 0) "未配置" else "$decodeCount 个字段",
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "›",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "可选。当此特征匹配成功后，将 BLE 明文字节按规则映射为标签。加密载荷仍保留十六进制原始格式。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                }
            }
            if (onDelete != null) {
                FieldwatchActionButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Delete, null)
                    Spacer(Modifier.padding(4.dp))
                    Text("删除特征")
                }
            }
        }
    }
    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除此特征？") },
            text = {
                Text(
                    if (initial.builtIn) {
                        "“${fleet.name}”是内置特征。删除它将移除其匹配逻辑、提示音书签以及过滤标签。在“设置”中点击“恢复默认特征库”可将其重新载入。"
                    } else {
                        "“${fleet.name}”将被移除。其匹配逻辑、提示音书签以及过滤标签将一并删除。此操作无法撤销。"
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Palette.fleet.forEachIndexed { index, argb ->
                val on = index == selected
                val fill = Color(argb).nightIf(LocalNightMode.current)
                Surface(
                    onClick = { onSelect(index) },
                    shape = RoundedCornerShape(8.dp),
                    color = fill,
                    border = BorderStroke(
                        width = if (on) 2.dp else 1.dp,
                        color = if (on) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (on) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = "已选颜色",
                                tint = if (fill.luminance() > 0.45f) {
                                    Color(0xFF12171C)
                                } else {
                                    Color.White
                                },
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleEditor(rule: MatchRule, onChange: (MatchRule) -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldwatchSwitch(
                rule.enabled,
                { onChange(rule.copy(enabled = it)) },
            )
            ExposedDropdownMenuBox(
                expanded,
                { expanded = it },
                Modifier
                    .weight(1f)
                    .padding(start = 8.dp, end = 4.dp),
            ) {
                FieldwatchDropdownField("规则类型", ruleKindLabel(rule.kind), expanded)
                ExposedDropdownMenu(expanded, { expanded = false }) {
                    RuleKind.entries.forEach { kind ->
                        DropdownMenuItem(text = { Text(ruleKindLabel(kind)) }, onClick = {
                            onChange(rule.copy(kind = kind))
                            expanded = false
                        })
                    }
                }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "删除规则") }
        }
        Column(
            Modifier.padding(top = 12.dp, start = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
        when (rule.kind) {
            RuleKind.OUI, RuleKind.MAC_PREFIX, RuleKind.NAME_CONTAINS, RuleKind.NAME_GLOB, RuleKind.SERVICE_UUID, RuleKind.VENDOR_IE_OUI -> {
                FieldwatchOutlinedField(
                    rule.text,
                    { onChange(rule.copy(text = it)) },
                    "规则匹配值",
                )
            }
            RuleKind.MANUFACTURER_ID -> {
                FieldwatchOutlinedField(
                    if (rule.companyId == 0) "" else "0x%04X".format(rule.companyId),
                    {
                        val parsed = it.removePrefix("0x").removePrefix("0X").toIntOrNull(16) ?: 0
                        onChange(rule.copy(companyId = parsed))
                    },
                    "厂商代码十六进制 (Company ID)",
                )
            }
            RuleKind.MANUFACTURER_DATA -> {
                FieldwatchOutlinedField(
                    if (rule.companyId == 0) "" else "0x%04X".format(rule.companyId),
                    {
                        val parsed = it.removePrefix("0x").removePrefix("0X").toIntOrNull(16) ?: 0
                        onChange(rule.copy(companyId = parsed))
                    },
                    "厂商代码十六进制 (Company ID)",
                )
                FieldwatchOutlinedField(
                    rule.dataPrefixHex,
                    { onChange(rule.copy(dataPrefixHex = it)) },
                    "数据前缀十六进制",
                )
            }
            RuleKind.SERVICE_DATA -> {
                FieldwatchOutlinedField(
                    rule.text,
                    { onChange(rule.copy(text = it)) },
                    "服务 UUID (留空表示任意，支持包含匹配)",
                )
                FieldwatchOutlinedField(
                    rule.dataPrefixHex,
                    { onChange(rule.copy(dataPrefixHex = it)) },
                    if (rule.text.isBlank()) "包含的十六进制" else "数据前缀十六进制",
                )
            }
            RuleKind.RADIO_KIND -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Wi-Fi", Modifier.padding(end = 8.dp))
                    FieldwatchSwitch(rule.radio != RadioKind.BLE, { onChange(rule.copy(radio = if (it) RadioKind.WIFI else RadioKind.BLE)) })
                }
            }
            RuleKind.HIDDEN_SSID -> Text("匹配隐藏 SSID", style = MaterialTheme.typography.bodySmall)
        }
        }
    }
}

private fun ruleKindLabel(kind: RuleKind): String = when (kind) {
    RuleKind.OUI -> "OUI 厂商"
    RuleKind.MAC_PREFIX -> "MAC 地址前缀"
    RuleKind.NAME_CONTAINS -> "名称包含"
    RuleKind.NAME_GLOB -> "名称通配符 (Glob)"
    RuleKind.SERVICE_UUID -> "服务 UUID"
    RuleKind.SERVICE_DATA -> "服务数据 (Service Data)"
    RuleKind.MANUFACTURER_ID -> "厂商 ID (Manufacturer ID)"
    RuleKind.MANUFACTURER_DATA -> "厂商自定义数据"
    RuleKind.RADIO_KIND -> "无线电类型"
    RuleKind.HIDDEN_SSID -> "隐藏 SSID"
    RuleKind.VENDOR_IE_OUI -> "厂商专有标签 OUI (Vendor IE)"
}

private fun compactLine(
    size: androidx.compose.ui.unit.TextUnit,
    line: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontSize = size,
    lineHeight = line,
    fontWeight = weight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)
