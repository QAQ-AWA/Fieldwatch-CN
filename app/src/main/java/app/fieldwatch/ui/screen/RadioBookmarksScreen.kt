package app.fieldwatch.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import app.fieldwatch.ui.component.FieldwatchActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import app.fieldwatch.ui.component.FieldwatchSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.fieldwatch.domain.MacUtil
import app.fieldwatch.domain.RadioBookmarks
import app.fieldwatch.domain.RadioKind
import app.fieldwatch.domain.WatchTarget
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.RadioKindMark
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioBookmarksScreen(
    state: FieldwatchUi,
    vm: FieldwatchViewModel,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
) {
    val radios = RadioBookmarks.radios(state.watchlist)
    val liveKeys = state.devices.filter { !it.gone }.map { it.key }.toSet()
    val demoMode = state.settings.demoMode
    var clearAll by remember { mutableStateOf(false) }
    var renameId by remember { mutableStateOf<String?>(null) }
    val renameTarget = radios.firstOrNull { it.id == renameId }

    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = {
            NestedTopBar(
                title = "已命名设备 (${radios.size})",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "按唯一 MAC 地址绑定。自定义名称直接显示在实时监控中。观测备注显示在详情页和研判报告中。可单独开启警报（提示音/语音/闪光）。在“过滤器”中开启“仅限已命名设备”可隐藏其余设备。特征库关注项在“特征库”中维护。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (radios.isEmpty()) {
                item {
                    Text(
                        "暂无已命名设备。在设备详情中设置自定义备注名，或点击右上角书签将其加入关注。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            } else {
                items(radios, key = { it.id }) { row ->
                    val parsed = row.deviceKey?.let { RadioBookmarks.parseKey(it) }
                    val kind = parsed?.first ?: RadioKind.BLE
                    val mac = parsed?.second ?: row.deviceKey.orEmpty()
                    val onAir = row.deviceKey in liveKeys
                    BookmarkCard(
                        row = row,
                        kind = kind,
                        mac = MacUtil.screenMac(mac, demoMode),
                        onAir = onAir,
                        onOpen = {
                            val key = row.deviceKey ?: return@BookmarkCard
                            if (onAir) onOpen(key)
                        },
                        onAlert = { on -> vm.setRadioAlert(row.id, on) },
                        onRename = { renameId = row.id },
                        onRemove = { vm.removeRadioBookmark(row.id) },
                    )
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    FieldwatchActionButton(
                        onClick = { clearAll = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("清空全部 ${radios.size} 台已命名设备")
                    }
                }
            }
        }
    }

    if (clearAll) {
        AlertDialog(
            onDismissRequest = { clearAll = false },
            title = { Text("清空已命名设备？") },
            text = {
                Text("将移除 ${radios.size} 台已命名设备。特征库关注项仍会保留。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.clearRadioBookmarks()
                        clearAll = false
                    },
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { clearAll = false }) { Text("取消") }
            },
        )
    }
    if (renameTarget != null) {
        var draft by remember(renameTarget.id) { mutableStateOf(renameTarget.label) }
        var notesDraft by remember(renameTarget.id) { mutableStateOf(renameTarget.observerNotes) }
        AlertDialog(
            onDismissRequest = { renameId = null },
            title = { Text("编辑已命名设备") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldwatchOutlinedField(
                        value = draft,
                        onValueChange = { draft = it.take(RadioBookmarks.MAX_NAME) },
                        label = "自定义备注名",
                    )
                    FieldwatchOutlinedField(
                        value = notesDraft,
                        onValueChange = { notesDraft = it.take(RadioBookmarks.MAX_NOTES) },
                        label = "观测备注",
                        singleLine = false,
                        minLines = 3,
                        supportingText = "${notesDraft.trim().length}/${RadioBookmarks.MAX_NOTES}",
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.updateNamedRadio(renameTarget.id, draft, notesDraft)
                        renameId = null
                    },
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameId = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun BookmarkCard(
    row: WatchTarget,
    kind: RadioKind,
    mac: String,
    onAir: Boolean,
    onOpen: () -> Unit,
    onAlert: (Boolean) -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onAir) Modifier.clickable(onClick = onOpen) else Modifier),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioKindMark(kind, size = 18.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.label.ifBlank { mac },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    mac,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (onAir) "当前在线 — 点击查看详情" else "本次会话未侦听到",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val obs = row.observerNotes.trim()
                if (obs.isNotEmpty()) {
                    Text(
                        obs,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    "警报",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FieldwatchSwitch(checked = row.alert, onCheckedChange = onAlert)
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Outlined.Edit, "编辑", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Outlined.Delete, "移除", modifier = Modifier.size(20.dp))
            }
        }
    }
}
