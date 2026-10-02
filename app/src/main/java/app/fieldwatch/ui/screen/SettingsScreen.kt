package app.fieldwatch.ui.screen

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import app.fieldwatch.ui.component.FieldwatchFilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import app.fieldwatch.ui.component.FieldwatchActionButton
import app.fieldwatch.ui.component.FieldwatchOutlinedField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import app.fieldwatch.ui.component.FieldwatchSlider
import androidx.compose.material3.Surface
import app.fieldwatch.ui.component.FieldwatchSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.fieldwatch.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.fieldwatch.domain.AlertVoiceWhat
import app.fieldwatch.domain.AppSettings
import app.fieldwatch.domain.ScanIntensity
import app.fieldwatch.domain.TakDefaults
import app.fieldwatch.domain.TakFeedStatus
import app.fieldwatch.domain.TakPublish
import app.fieldwatch.domain.TakUdpPreset
import app.fieldwatch.radio.WifiRadio
import app.fieldwatch.ui.NestedTabInsets
import app.fieldwatch.ui.NestedTopBar
import app.fieldwatch.ui.FieldwatchUi
import app.fieldwatch.ui.FieldwatchViewModel
import app.fieldwatch.ui.component.SectionCard
import app.fieldwatch.ui.component.FieldwatchFilterChip
import app.fieldwatch.ui.component.StableCaption
import app.fieldwatch.ui.component.StickyHeight
import java.net.Inet4Address
import java.net.NetworkInterface

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    state: FieldwatchUi,
    vm: FieldwatchViewModel,
    onRadioBookmarks: () -> Unit,
    onShowLiveTour: () -> Unit = {},
) {
    val context = LocalContext.current
    val settings = state.settings
    val saveSignatures = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(vm::saveSignaturesToUri) }
    val importSignatures = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::importSignaturesFromUri) }
    val saveSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(vm::saveSettingsToUri) }
    val importSettings = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::importSettingsFromUri) }
    var confirmRestore by remember { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = NestedTabInsets,
        topBar = { NestedTopBar("设置") },
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("外观显示") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("夜间红光模式", Modifier.weight(1f))
                FieldwatchSwitch(settings.nightMode, { on -> vm.updateSettings { it.copy(nightMode = on) } })
            }
            Text(
                "默认关闭。黑底红光战术显示，防止芯片、文字和信号标记在暗处作业环境下发出绿色或蓝色的刺眼亮光。背景保持暗黑，手机屏幕亮度设置不变。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("保持屏幕常亮", Modifier.weight(1f))
                FieldwatchSwitch(settings.keepScreenOn, { on -> vm.updateSettings { it.copy(keepScreenOn = on) } })
            }
            Text(
                "默认开启。防止 Fieldwatch 打开时屏幕休眠，避免息屏时 BLE 扫描被系统挂起。离开应用后扫描仍在后台通知中继续运行。将手机放入衣袋时建议关闭。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("隐私演示模式", Modifier.weight(1f))
                FieldwatchSwitch(settings.demoMode, { on -> vm.updateSettings { it.copy(demoMode = on) } })
            }
            Text(
                "在实时、雷达、时间线、设备详情、寻踪、命名设备和关注卡片中将所有 MAC 地址的后 3 个字节脱敏隐藏为 **:**:**，使屏幕截图与态势研判报告不泄露完整设备物理地址。GPS 最后定位和研判报告/AI 导出/详情分享中的坐标将显示为“已遮蔽”，且报告中将略去街道名称；保留前 3 个字节（OUI / 厂商前缀）。默认关闭。开启“在线地名与地图”时，轨迹地图仍可加载。后台日志、规则匹配、过滤、寻踪测距算法、随行移动和已存特征依然使用真实 MAC 与 GPS。若开启了 TAK / CoT 数据流，在此模式下将自动暂停发送，以免将完整 MAC 与坐标广播至局域网。在屏幕上需要查看完整物理地址或真实坐标时请关闭此选项。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("扫描设置") {
            val label = when (settings.intensity) {
                ScanIntensity.SAVER -> "省电模式"
                ScanIntensity.BALANCED -> "均衡模式"
                ScanIntensity.PERFORMANCE -> "高性能模式"
            }
            Text("扫描强度  ·  $label")
            FieldwatchSlider(
                value = settings.intensity.ordinal.toFloat(),
                onValueChange = { v ->
                    val next = ScanIntensity.entries[v.toInt().coerceIn(0, 2)]
                    vm.updateSettings { it.copy(intensity = next) }
                },
                valueRange = 0f..2f,
                steps = 1,
            )
            Text(
                "Wi-Fi 属于批处理射频：手机一次性抓取周围所有接入点 (AP)，随后必须等待。高性能模式大约每 30 秒发起一次请求——这是在 Android 系统“每 2 分钟最多 4 次扫描”限制下的最快频率。两次扫描间隙期间 BLE 数据流仍保持不间断监听。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StableCaption(
                state.throttleHint.ifBlank { " " },
                "Wi-Fi 等待系统调度",
                "Wi-Fi 正在扫描",
                "Wi-Fi 下次扫描 99s",
                " ",
            )

            val lifecycleOwner = LocalLifecycleOwner.current
            var osThrottled by remember { mutableStateOf(WifiRadio.osScanThrottled(context)) }
            var backgroundAllowed by remember { mutableStateOf(isBackgroundUsageAllowed(context)) }
            var unrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
            var needDevOptions by remember { mutableStateOf(false) }
            var batteryGate by remember { mutableStateOf<BatteryAndroidGate?>(null) }
            DisposableEffect(lifecycleOwner) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        osThrottled = WifiRadio.osScanThrottled(context)
                        backgroundAllowed = isBackgroundUsageAllowed(context)
                        unrestricted = isIgnoringBatteryOptimizations(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(obs)
                onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
            }
            val fastActive = settings.wifiFastScan && !osThrottled
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("加速 Wi-Fi AP 扫描", Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = settings.wifiFastScan,
                    onCheckedChange = { on ->
                        if (!on) {
                            vm.updateSettings { it.copy(wifiFastScan = false) }
                        } else if (!osThrottled) {
                            vm.updateSettings { it.copy(wifiFastScan = true) }
                        } else {
                            needDevOptions = true
                        }
                    },
                )
            }
            StableCaption(
                when {
                    Build.VERSION.SDK_INT < 30 ->
                        "需要 Android 11 及以上版本，以便 Fieldwatch 读取系统是否仍在限制扫描频率。当前手机无法确认此状态，因此开关保持关闭。"
                    fastActive ->
                        "已开启。Fieldwatch 大约每 8 秒请求一次新的 AP 列表。这会消耗更多电量并增加发热。如果系统开始拒绝扫描请求，将自动回退降低频率。"
                    settings.wifiFastScan && osThrottled ->
                        "已保存开启，但尚未生效——Android 系统的“Wi-Fi 扫描限制”仍处于启用状态。请在“开发者选项”中关闭该限制，然后返回此处。"
                    else ->
                        "原生 Android 限制每 2 分钟仅允许约 4 次 AP 扫描。只有在“开发者选项”中关闭“Wi-Fi 扫描限制”后，加速扫描才能生效。Fieldwatch 在开启前会检查该系统开关，且无法代您修改系统配置。"
                },
                "需要 Android 11 及以上版本，以便 Fieldwatch 读取系统是否仍在限制扫描频率。当前手机无法确认此状态，因此开关保持关闭。",
                "已开启。Fieldwatch 大约每 8 秒请求一次新的 AP 列表。这会消耗更多电量并增加发热。如果系统开始拒绝扫描请求，将自动回退降低频率。",
                "已保存开启，但尚未生效——Android 系统的“Wi-Fi 扫描限制”仍处于启用状态。请在“开发者选项”中关闭该限制，然后返回此处。",
                "原生 Android 限制每 2 分钟仅允许约 4 次 AP 扫描。只有在“开发者选项”中关闭“Wi-Fi 扫描限制”后，加速扫描才能生效。Fieldwatch 在开启前会检查该系统开关，且无法代您修改系统配置。",
            )
            if (needDevOptions) {
                AlertDialog(
                    onDismissRequest = { needDevOptions = false },
                    title = { Text("需要开发者选项") },
                    text = {
                        Text(
                            if (Build.VERSION.SDK_INT < 30) {
                                "当前设备 Android 版本低于 11，Fieldwatch 无法读取系统的 Wi-Fi 扫描限制状态。加速 AP 扫描将保持关闭。"
                            } else {
                                "Android 系统当前正在限制 Wi-Fi 扫描频率（每 2 分钟约 4 次）。在该限制关闭之前，Fieldwatch 不会激活加速扫描。\n\n" +
                                    "请启用“开发者选项”（在“关于手机”中连续点击“版本号”7 次），然后前往“系统设置 → 开发者选项 → 关闭 Wi-Fi 扫描限制（Wi-Fi 扫描调节）”。完成后返回此处重新打开此开关。"
                            },
                        )
                    },
                    confirmButton = {
                        if (Build.VERSION.SDK_INT >= 30) {
                            TextButton(
                                onClick = {
                                    needDevOptions = false
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
                                    }
                                },
                            ) { Text("打开开发者选项") }
                        } else {
                            TextButton(onClick = { needDevOptions = false }) { Text("确定") }
                        }
                    },
                    dismissButton = {
                        if (Build.VERSION.SDK_INT >= 30) {
                            TextButton(onClick = { needDevOptions = false }) { Text("暂不设置") }
                        }
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("允许后台使用", Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = backgroundAllowed,
                    onCheckedChange = { batteryGate = BatteryAndroidGate.BACKGROUND },
                )
            }
            Text(
                "映射 Android 系统的“允许后台使用”权限。轻触可打开 Fieldwatch 的系统电池设置页。返回后状态将自动刷新。若关闭此项，离开应用后系统可能会立即终止射频扫描。此项不同于“保持屏幕常亮”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("电池无限制 (不受优化)", Modifier.weight(1f))
                FieldwatchSwitch(
                    checked = unrestricted,
                    onCheckedChange = { batteryGate = BatteryAndroidGate.UNRESTRICTED },
                )
            }
            Text(
                "映射 Android 系统的电池“无限制”策略（非“优化”）。部分手机（如三星等）界面可能未直接展开三挡选项。若仅看到“允许后台使用”，请轻触该行文字进入二级菜单并选择“无限制”。返回后将自动更新状态。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (batteryGate != null) {
                val background = batteryGate == BatteryAndroidGate.BACKGROUND
                AlertDialog(
                    onDismissRequest = { batteryGate = null },
                    title = {
                        Text(if (background) "允许后台使用" else "电池无限制")
                    },
                    text = {
                        Text(
                            if (background) {
                                "即将跳转至 Fieldwatch 的系统电池管理页面。请开启“允许后台使用”开关。返回应用后将同步该设置。"
                            } else {
                                "部分手机（如三星）未直接展示“无限制/受优化/受限”单选项。若只看到“允许后台使用”，请轻触整行文字（而非开关本身）进入详情页，然后勾选“无限制”。返回后将同步该设置。"
                            },
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val gate = batteryGate
                                batteryGate = null
                                openAppBatteryPage(
                                    context,
                                    highlightBackground = gate == BatteryAndroidGate.BACKGROUND,
                                )
                            },
                        ) { Text("打开系统设置") }
                    },
                    dismissButton = {
                        TextButton(onClick = { batteryGate = null }) { Text("暂不设置") }
                    },
                )
            }
            }

            SectionCard("关注列表与警报") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("关注项警报总开关", Modifier.weight(1f))
                FieldwatchSwitch(settings.alertsEnabled, { on -> vm.updateSettings { it.copy(alertsEnabled = on) } })
            }
            Text(
                "默认开启。星标关注特征与指定设备警报的总开关。关闭后：不会触发蜂鸣、震动、闪烁、列表跳转或通知栏提醒。星标与自定义命名依然有效——只是在对应设备出现时不会打扰通知。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val radioWatchN = state.watchlist.count { it.deviceKey != null }
            FieldwatchActionButton(
                onClick = onRadioBookmarks,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("自定义命名设备 ($radioWatchN)") }
            Text(
                "为指定 MAC 地址设置自定义备注名称，警报开关可单独配置。在“过滤规则 → 仅限自定义命名设备”中可将它们单独筛选到实时视图。特征库的关注设置位于“特征库”标签页中。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("匹配关注特征时蜂鸣提示", Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.alertBeep,
                    { on -> vm.updateSettings { it.copy(alertBeep = on) } },
                    enabled = settings.alertsEnabled,
                )
            }
            Text(
                "当已关注的特征或设备首次出现或离开后重新出现时，通过媒体音量播放双声蜂鸣提示音。原地持续存在的设备不会重复鸣叫。此功能独立于语音播报——可选择蜂鸣、语音或两者兼用。若听不到声音请调大媒体音量，然后轻触下方的“测试警报”。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("匹配关注特征时语音播报", Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.alertVoice,
                    { on -> vm.updateSettings { it.copy(alertVoice = on) } },
                    enabled = settings.alertsEnabled,
                )
            }
            Text(
                "默认开启。使用与蜂鸣相同的媒体音量进行语音合成播报。独立于蜂鸣：两者皆开启时语音紧随蜂鸣播放；蜂鸣关闭时仅播报语音。此项非寻踪模式。若正在播报前一条短语，新的提示将自动跳过。未安装 TTS 语音引擎的设备在开启蜂鸣时仍会正常响铃。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("播报内容配置", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AlertVoiceWhat.entries.forEach { item ->
                    FieldwatchFilterChip(
                        selected = settings.alertVoiceWhat == item,
                        onClick = { vm.updateSettings { it.copy(alertVoiceWhat = item) } },
                        enabled = settings.alertsEnabled && settings.alertVoice,
                        label = { Text(item.label()) },
                    )
                }
            }
            Text(
                "对于特征关注项：“类别”播报实时图标对应的分类（如：寻物标签、音频等）；“特征”播报具体特征名称（如：Apple AirTags、Axon 等）；“类别 + 特征”（默认）将两者一并播报。对于已开启警报的命名设备，始终播报其自定义备注名。轻触“测试警报”可试听当前设置的播报效果。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::testWatchBeep,
                modifier = Modifier.fillMaxWidth(),
                enabled = settings.alertsEnabled && (settings.alertBeep || settings.alertVoice),
            ) { Text("测试警报") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("自动跳转至新检出的关注设备", Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.snapToBeep,
                    { on -> vm.updateSettings { it.copy(snapToBeep = on) } },
                    enabled = settings.alertsEnabled && (settings.alertBeep || settings.alertVoice),
                )
            }
            Text(
                "当新的关注特征或设备出现时，“实时”列表会自动滚动到该设备行以便操作员观察高亮闪烁。支持配合蜂鸣或语音使用。弱信号设备通常排在强度列表的底部。若您不希望列表自动滑动移位，可关闭此选项。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("系统通知栏卡片", Modifier.weight(1f))
                FieldwatchSwitch(
                    settings.alertShade,
                    { on -> vm.updateSettings { it.copy(alertShade = on) } },
                    enabled = settings.alertsEnabled,
                )
            }
            Text(
                "可选。在关注的无线电设备出现时，在系统通知栏发送静音卡片提醒。默认关闭——蜂鸣与高亮闪烁已足够醒目，省略通知栏卡片可使后台扫描循环更加轻量高效。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("位置定位") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("使用 GPS 标记检出位置", Modifier.weight(1f))
                FieldwatchSwitch(settings.tagLocation, { on -> vm.updateSettings { it.copy(tagLocation = on) } })
            }
            Text(
                "默认开启。请求实时 GPS/网络位置更新，并为每次侦听打上坐标时间戳（用于实时详情、随行移动判定、态势研判以及日志中的经纬度）。超过 30 秒的“上次已知位置”将被忽略。该坐标是操作员侦听到信号时本机所在的位置，而非目标无线电设备的物理定位。请使用高精度定位模式，否则移动轨迹距离将保持为 0。若不希望在日志中记录操作员自身的坐标，可关闭此项。TAK “本机侦听 (here)”图钉同样依赖此项；而广播负载自带的位置数据（如无人机 Remote ID）则不受影响。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("在线地名反查与底图", Modifier.weight(1f))
                FieldwatchSwitch(settings.onlineLookup, { on -> vm.updateSettings { it.copy(onlineLookup = on) } })
            }
            Text(
                "默认开启。当手机具备网络连接时，研判报告与 AI 导出将对 GPS 坐标进行地理逆编码解析为具体街道/城市，“报告 → 移动轨迹”将在轨迹下方加载 OpenStreetMap 瓦片底图。不依赖任何 Fieldwatch 云端服务，无需 API Key。离线或无可用地理编码器时：研判报告仅保留纯坐标数据，轨迹界面保持正北朝上的矢量折线，不弹出报错。关闭此项可防止在报告与轨迹中包含街道名称和地图底图。态势研判、导出与清空日志等功能位于“报告”标签页中。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            }

            SectionCard("TAK / CoT 数据分发") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("TAK / CoT 数据推流", Modifier.weight(1f))
                FieldwatchSwitch(settings.takEnabled, { on -> vm.updateSettings { it.copy(takEnabled = on) } })
            }
            Text(
                "默认关闭。向 ATAK、WinTAK 或 iTAK 发送目标光标 (Cursor-on-Target) UDP 标记图钉。" +
                    "本机 (${TakDefaults.LOOPBACK}:${TakDefaults.PORT}) 用于同设备上的 ATAK CIV；" +
                    "局域网组播地址为 ${TakDefaults.SA_HOST}:${TakDefaults.SA_PORT}；" +
                    "自定义支持单播 IPv4 地址或主机名。仅支持 UDP 广播/组播——TAK 服务器的 TCP 8087 端口不属于此类直连推流。" +
                    "本机侦听标记放置在信号最强时（最接近时）本机的 GPS 坐标处，并附带 (here) 标注。" +
                    "离开目标时不会拉扯标记，只有接收到更强信号时才会更新位置。每隔约 10 秒刷新一次心跳包以防止 ATAK 自动丢弃。" +
                    "广播自带经纬度（如原生 Remote ID）直接标注在无人机飞行位置上；同一个 Remote ID " +
                    "维护一个移动航迹标记（以无人机 UAS ID 标识，不受 BLE MAC 轮换影响）。" +
                    "解析出的飞手位置将作为第二图钉显示。消失的无线电设备在 ATAK 上会被主动清除，而无需等待 120 秒超时。" +
                    "在 ATAK 中轻触标记可查看备注（名称、MAC、RSSI、特征匹配）。" +
                    "注意：本功能非无线电测向，非 Remote ID 广播插件。隐私演示模式下会自动暂停推流。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (settings.takEnabled && settings.demoMode) {
                Text(
                    "隐私演示模式处于开启状态——数据流已暂停，以避免向外发送完整 MAC 地址与真实坐标。关闭隐私演示模式后方可恢复推流。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (settings.takEnabled) {
                TakFeedSettings(settings, vm, state.takStatus)
            }
            }

            SectionCard("日志记录") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("将检出数据写入存储", Modifier.weight(1f))
                FieldwatchSwitch(settings.loggingEnabled, { on -> vm.updateSettings { it.copy(loggingEnabled = on) } })
            }
            StableCaption(
                if (settings.loggingEnabled) {
                    "日志记录已开启。新检出的无线电设备将追加写入轮转日志文件中。"
                } else {
                    "日志记录已关闭。扫描仍会运行，但在重新开启前不会向文件写入新数据。"
                },
                "日志记录已开启。新检出的无线电设备将追加写入轮转日志文件中。",
                "日志记录已关闭。扫描仍会运行，但在重新开启前不会向文件写入新数据。",
            )
            Text(
                "磁盘上的轮转存储格式为 JSON Lines（每行记录一次侦听事件）。在“报告 → 日志导出”中分享或保存时，可导出为 CSV、JSON Lines、GPX、KML 或 WiGLE 格式。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            var rotateDrag by remember { mutableIntStateOf(settings.logRotateKb) }
            var rotateDragging by remember { mutableStateOf(false) }
            LaunchedEffect(settings.logRotateKb) {
                if (!rotateDragging) rotateDrag = settings.logRotateKb
            }
            Text("单个日志文件大小上限: $rotateDrag KB")
            FieldwatchSlider(
                value = rotateDrag.toFloat(),
                onValueChange = {
                    rotateDragging = true
                    rotateDrag = it.toInt().coerceIn(128, 4096)
                },
                onValueChangeFinished = {
                    vm.updateSettings { s -> s.copy(logRotateKb = rotateDrag) }
                    rotateDragging = false
                },
                valueRange = 128f..4096f,
            )
            var staleDrag by remember { mutableIntStateOf(settings.staleSec) }
            var staleDragging by remember { mutableStateOf(false) }
            LaunchedEffect(settings.staleSec) {
                if (!staleDragging) staleDrag = settings.staleSec
            }
            Text("设备静默超时判定: ${staleDrag} 秒")
            FieldwatchSlider(
                value = staleDrag.toFloat(),
                onValueChange = {
                    staleDragging = true
                    staleDrag = it.toInt().coerceIn(15, 180)
                },
                onValueChangeFinished = {
                    vm.updateSettings { s -> s.copy(staleSec = staleDrag) }
                    rotateDragging = false
                },
                valueRange = 15f..180f,
            )
            StickyHeight("log-stats") {
                Text(
                    "本次会话已记录 ${state.logLines} 行  ·  磁盘占用 ${vm.logBytes() / 1024} KB。" +
                        "分享、保存及重置/清空日志操作位于“报告”标签页中。",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            }

            SectionCard("特征库管理") {
            Text(
                "导出特征库目录（包含内置特征及您添加或编辑的全部规则），可用于与其他 Fieldwatch 设备共享或作为备份。导入会追加新特征项与匹配规则，不会删除现有内容；相同 ID 或完全相同的匹配规则会自动跳过，因此同一特征包可重复导入。“从 GitHub 更新内置特征库”将从项目仓库拉取最新的 v2 特征包更新内置特征（含重点关注项）；您的星标关注、设置选项及自定义添加的特征均会完整保留。此操作需要联网。离线环境下可选择“从文件导入特征”。下方“恢复默认”仍会清空所有自定义内容。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::startSignatureShare,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出特征库 (分享)") }
            FieldwatchActionButton(
                onClick = { saveSignatures.launch(vm.suggestedSignaturesName()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存特征库至存储卡 / 本地存储…") }
            FieldwatchActionButton(
                onClick = {
                    importSignatures.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入特征库文件…") }
            FieldwatchActionButton(
                onClick = vm::updateStockCatalogFromGitHub,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("从 GitHub 更新内置特征库") }

            FieldwatchActionButton(
                onClick = { confirmRestore = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("恢复默认特征库与预设")
            }
            }

            SectionCard("设置备份与恢复") {
            Text(
                "备份各项设置开关、当前过滤规则、预设模板、自定义命名设备以及特征星标关注列表。不包含特征库本身（特征库请使用“导出特征库”），亦不包含日志或 GPS 轨迹。导入时将用备份覆盖本机上述配置，特征库保持不变。适用于换机迁移或恢复出厂设置后还原。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FieldwatchActionButton(
                onClick = vm::startSettingsShare,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出设置 (分享)") }
            FieldwatchActionButton(
                onClick = { saveSettings.launch(vm.suggestedSettingsName()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("保存设置至存储卡 / 本地存储…") }
            FieldwatchActionButton(
                onClick = {
                    importSettings.launch(arrayOf("application/json", "text/plain", "*/*"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导入设置配置文件…") }
            }

            FieldwatchActionButton(
                onClick = onShowLiveTour,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("显示实时界面操作导览") }
            Text(
                "在“实时”主界面展示功能引导气泡：包含“调谐/视图模式”（雷达、列表、按类别分组）、暂停、过滤规则、特征库、报告、设置等引导说明。初次同意免责许可后会默认展示，轻触此按钮可重新浏览。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                "Fieldwatch ${app.fieldwatch.BuildConfig.VERSION_NAME}  ·  特征库版本 ${state.catalogVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "仅限被动式 Wi-Fi 与 BLE 射频监听。" +
                    "原生 Android 无法以混杂模式监听 Wi-Fi 终端 Station 数据；无线电仅能侦听暴露在空中的接入点 (AP) 与 BLE 广播设备。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val footerLifecycle = LocalLifecycleOwner.current
            var ipv4 by remember { mutableStateOf(localIpv4Addresses()) }
            DisposableEffect(footerLifecycle) {
                val obs = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) ipv4 = localIpv4Addresses()
                }
                footerLifecycle.lifecycle.addObserver(obs)
                onDispose { footerLifecycle.lifecycle.removeObserver(obs) }
            }
            Text(
                if (ipv4.isEmpty()) {
                    "本机 IPv4 地址  ·  无网络"
                } else {
                    "本机 IPv4 地址  ·  ${ipv4.joinToString("  ·  ")}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            CreditFooter()
        }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("恢复默认设置？") },
            text = {
                Text(
                    "将重置特征库（恢复内置特征项、类别配色、解码字段）、恢复默认星标、" +
                        "默认过滤标签以及默认设置开关。您保存的自定义特征与预设标签将被清空。" +
                        "如需备份，请先执行“导出特征库”和“导出设置”。此操作不可撤销。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRestore = false
                        vm.restoreDefaults()
                    },
                ) { Text("恢复默认") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CreditFooter() {
    val context = LocalContext.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Copyright (c) 2026 Off Grid Pete LLC. All rights reserved.",
            style = MaterialTheme.typography.labelSmall,
            color = muted,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocialChip(
                icon = R.drawable.ic_instagram,
                label = "@OffGridPete",
                tint = muted,
                onClick = { openUrl(context, "https://instagram.com/OffGridPete") },
            )
            SocialChip(
                icon = R.drawable.ic_x,
                label = "@OGridPete",
                tint = muted,
                onClick = { openUrl(context, "https://x.com/OGridPete") },
            )
        }
    }
}

@Composable
private fun SocialChip(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(99.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TakFeedSettings(settings: AppSettings, vm: FieldwatchViewModel, status: TakFeedStatus) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    var hostText by remember { mutableStateOf(settings.takHost) }
    var portText by remember { mutableStateOf(settings.takPort.toString()) }
    LaunchedEffect(settings.takHost) { hostText = settings.takHost }
    LaunchedEffect(settings.takPort) { portText = settings.takPort.toString() }
    val preset = TakPublish.udpPreset(settings.takHost, settings.takPort)
    Text("发送目标", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldwatchFilterChip(
            selected = preset == TakUdpPreset.THIS_PHONE,
            onClick = {
                val (host, port) = TakPublish.applyPreset(TakUdpPreset.THIS_PHONE)
                vm.updateSettings { it.copy(takHost = host, takPort = port) }
            },
            enabled = !settings.demoMode,
            label = { Text("本机 (ATAK CIV)") },
        )
        FieldwatchFilterChip(
            selected = preset == TakUdpPreset.LAN_MULTICAST,
            onClick = {
                val (host, port) = TakPublish.applyPreset(TakUdpPreset.LAN_MULTICAST)
                vm.updateSettings { it.copy(takHost = host, takPort = port) }
            },
            enabled = !settings.demoMode,
            label = { Text("局域网组播 (SA)") },
        )
        FieldwatchFilterChip(
            selected = preset == TakUdpPreset.CUSTOM,
            onClick = {
                if (preset != TakUdpPreset.CUSTOM) {
                    val (host, port) = TakPublish.applyPreset(TakUdpPreset.CUSTOM)
                    vm.updateSettings { it.copy(takHost = host, takPort = port) }
                }
            },
            enabled = !settings.demoMode,
            label = { Text("自定义目标") },
        )
    }
    Text(
        "本机：${TakDefaults.LOOPBACK}:${TakDefaults.PORT}（本机同装的 ATAK CIV）。" +
            "局域网组播：${TakDefaults.SA_HOST}:${TakDefaults.SA_PORT}（同一 Wi-Fi 下的其他 ATAK 设备）。" +
            "自定义：输入单播 IPv4 地址或主机名。仅限 UDP。不支持直接连 TAK 服务器的 TCP 8087。" +
            "若选择本机后 ATAK 未能显示标绘，请选用自定义模式并填入底部显示的本机 Wi-Fi IPv4 地址和端口 ${TakDefaults.PORT}。",
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
    FieldwatchOutlinedField(
        value = hostText,
        onValueChange = { value ->
            hostText = value
            val trimmed = value.trim()
            if (trimmed.isNotEmpty()) {
                vm.updateSettings { it.copy(takHost = trimmed) }
            }
        },
        label = "主机 / IP 地址",
        placeholder = TakDefaults.HOST,
        enabled = !settings.demoMode,
    )
    FieldwatchOutlinedField(
        value = portText,
        onValueChange = { value ->
            val filtered = value.filter { it.isDigit() }.take(5)
            portText = filtered
            filtered.toIntOrNull()?.let { n ->
                if (n in 1..65_535) {
                    vm.updateSettings { it.copy(takPort = n) }
                }
            }
        },
        label = "端口",
        placeholder = TakDefaults.PORT.toString(),
        supportingText = "UDP 协议。ATAK CIV 默认 ${TakDefaults.PORT}。态势组播 ${TakDefaults.SA_PORT}。非 TCP 8087。",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = !settings.demoMode,
    )
    Text(takStatusLine(status), style = MaterialTheme.typography.bodySmall, color = muted)
    Text("发送内容范围", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldwatchFilterChip(
            selected = settings.takAttention,
            onClick = { vm.updateSettings { it.copy(takAttention = !it.takAttention) } },
            enabled = !settings.demoMode,
            label = { Text("重点关注对象") },
        )
        FieldwatchFilterChip(
            selected = settings.takPayloadFix,
            onClick = { vm.updateSettings { it.copy(takPayloadFix = !it.takPayloadFix) } },
            enabled = !settings.demoMode,
            label = { Text("载荷自带坐标") },
        )
        FieldwatchFilterChip(
            selected = settings.takWatchlist,
            onClick = { vm.updateSettings { it.copy(takWatchlist = !it.takWatchlist) } },
            enabled = !settings.demoMode,
            label = { Text("关注列表项") },
        )
        FieldwatchFilterChip(
            selected = settings.takAllSignatures,
            onClick = { vm.updateSettings { it.copy(takAllSignatures = !it.takAllSignatures) } },
            enabled = !settings.demoMode,
            label = { Text("全部已知特征") },
        )
    }
    Text(
        "多选标签。重点关注（默认开）：执法仪、智能眼镜、录音摄像穿戴、渗透工具、公共安全 AP。" +
            "载荷自带坐标（默认开）：从解码映射中解析出的广播经纬度——原生 Remote ID 必需项（因其无重点关注标记）。" +
            "关注列表（默认关）：已加星标的特征及已开警报的命名设备。" +
            "全部已知特征（默认关）：所有已识别特征的设备——在密集场所较为嘈杂。未匹配特征的普通设备绝不会发送。" +
            "生成图钉必须具备有效坐标（广播载荷自带或本机 GPS 实时定位）。" +
            "“本机侦听”记录信号最强点而非最后一点，呼号以 (here) 结尾。" +
            "Remote ID 维护单个无人机航迹标记（以 UAS ID 标识），若解析出飞手位置则另标飞手图钉。",
        style = MaterialTheme.typography.bodySmall,
        color = muted,
    )
}

private fun takStatusLine(status: TakFeedStatus): String {
    if (status.paused) return "推流状态  ·  已暂停 (隐私模式)"
    if (status.error != null) {
        val whenAt = takStatusWhen(status.at)
        return "推流状态  ·  错误: ${status.error}" + if (whenAt.isNotEmpty()) "  ·  $whenAt" else ""
    }
    if (status.at <= 0L) {
        return "推流状态  ·  本次会话暂无发送记录"
    }
    val bits = ArrayList<String>(5)
    bits += "当前推流 ${status.onFeed}"
    bits += "已发送 ${status.sent}"
    if (status.gone > 0) {
        bits += "${status.gone} 个已移出"
    }
    if (status.dest.isNotBlank()) bits += status.dest
    val whenAt = takStatusWhen(status.at)
    if (whenAt.isNotEmpty()) bits += whenAt
    val head = "推流状态  ·  ${bits.joinToString("  ·  ")}"
    return if (status.detail.isNotBlank() && status.sent == 0 && status.gone == 0) {
        "$head  ·  ${status.detail}"
    } else {
        head
    }
}

private fun takStatusWhen(at: Long): String {
    if (at <= 0L) return ""
    return java.time.Instant.ofEpochMilli(at)
        .atZone(java.time.ZoneId.systemDefault())
        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
}

private fun localIpv4Addresses(): List<String> {
    val found = LinkedHashSet<String>()
    val nifs = runCatching {
        java.util.Collections.list(NetworkInterface.getNetworkInterfaces())
    }.getOrDefault(emptyList())
    for (nif in nifs) {
        if (!nif.isUp || nif.isLoopback) continue
        for (addr in java.util.Collections.list(nif.inetAddresses)) {
            if (addr is Inet4Address && !addr.isLoopbackAddress && !addr.isLinkLocalAddress) {
                addr.hostAddress?.let { found += it }
            }
        }
    }
    return found.toList()
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean =
    context.getSystemService(PowerManager::class.java)
        ?.isIgnoringBatteryOptimizations(context.packageName) == true

private fun isBackgroundUsageAllowed(context: Context): Boolean =
    context.getSystemService(ActivityManager::class.java)?.isBackgroundRestricted != true

private enum class BatteryAndroidGate { BACKGROUND, UNRESTRICTED }

/**
 * Fieldwatch’s per-app Battery page. Samsung keeps Allow background usage and
 * Unrestricted on this same screen. [highlightBackground] asks Settings to
 * focus the background-usage switch when the OEM supports it.
 */
private fun openAppBatteryPage(context: Context, highlightBackground: Boolean) {
    val pkgUri = Uri.fromParts("package", context.packageName, null)
    val attempts = listOf(
        Intent("android.settings.VIEW_ADVANCED_POWER_USAGE_DETAIL").apply {
            data = pkgUri
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("request_ignore_background_restriction", highlightBackground)
            if (!highlightBackground) {
                putExtra(":settings:fragment_args_key", "unrestricted_pref")
            }
        },
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = pkgUri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
    for (intent in attempts) {
        if (intent.resolveActivity(context.packageManager) == null) continue
        if (runCatching { context.startActivity(intent) }.isSuccess) return
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
