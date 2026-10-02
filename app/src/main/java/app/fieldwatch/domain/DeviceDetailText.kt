package app.fieldwatch.domain

import app.fieldwatch.radio.BleAdParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Plain-text dump of the device-detail screen. Same fields, no sparkline/presence art.
 * Not a legal identity.
 */
object DeviceDetailText {
    fun build(
        device: Sighting,
        signatureNames: List<String>,
        now: Long = System.currentTimeMillis(),
        attentionNotes: List<Pair<String, String>> = emptyList(),
        signatureNotes: List<Pair<String, String>> = emptyList(),
        fleets: List<Fleet> = emptyList(),
    ): String {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US)
        val facts = device.facts
        val title = device.listTitle(signatureNames)
        val guess = DeviceExplain.guess(device, signatureNames)
        val out = StringBuilder()

        fun line(label: String, value: String) {
            out.append(label).append(": ").append(value.trim()).append('\n')
        }
        fun section(title: String) {
            out.append('\n').append("## ").append(title).append('\n')
        }

        out.append("Fieldwatch 设备详情\n")
        out.append(iso.format(Date(now))).append('\n')
        out.append(
            "实验性功能。非法律意义上的身份认定。基于原生安卓无线电接口——仅展示系统所暴露的数据，不保证必定存在追踪器或摄像头。\n",
        )
        out.append('\n')
        out.append(title).append('\n')
        line("MAC 地址", device.mac)
        if (device.name.isNotBlank()) line("广播名称", device.name)

        out.append('\n')
        out.append("研判推测: ").append(guess.headline).append('\n')
        out.append(guess.because).append('\n')
        if (attentionNotes.isNotEmpty()) {
            section("重点关注")
            attentionNotes.forEach { (name, note) ->
                out.append("重点关注 ($name): ").append(note.trim()).append('\n')
            }
            out.append("仅为模式匹配，不代表身份，亦不作为安全定性结论。\n")
        }
        if (signatureNotes.isNotEmpty()) {
            section("备注信息")
            signatureNotes.forEach { (name, note) ->
                out.append(name).append(": ").append(note.trim()).append('\n')
            }
        }

        section("身份识别")
        line(
            "无线电类型",
            if (device.kind == RadioKind.WIFI) {
                "Wi-Fi 接入点 (AP 广播信标)"
            } else {
                "低功耗蓝牙 (BLE 广播设备)"
            },
        )
        line("地址类型", DeviceExplain.addressExplain(device))
        vendorLine(device)?.let { line("制造厂商", it.replace('\n', ' ')) }
            ?: line("OUI (厂商前缀)", "${device.oui} — 无 IEEE 匹配记录；随机地址通常无匹配厂商")
        if (device.hiddenSsid) {
            line("网络名称 (SSID)", "隐藏网络 — 接入点正在发送广播信标但未公开网络名称")
        }

        section("信号强度")
        if (device.gone) {
            line("当前接收强度 (RSSI)", "不可用")
            val last = Rssi.lastMeasured(device.rssi, device.rssiHistory)
            line("最近侦听", last?.let { "$it dBm" } ?: "不可用")
        } else {
            line("当前接收强度 (RSSI)", DeviceExplain.rssiExplain(device.rssi))
            out.append("越接近 0 dBm 说明信号越强，但并不代表实际物理距离。\n")
        }
        line("本次监测波动区间", Rssi.sessionRange(device.rssiMin, device.rssiMax, device.rssiHistory))
        facts.txPowerDbm?.let {
            line("声明发射功率 (Tx Power)", "$it dBm — 设备声明的自身发射功率，非物理距离")
        }
        if (device.channel != 0 || device.frequencyMhz != 0) {
            line(
                "信道 / 频率",
                buildString {
                    if (device.channel != 0) append("${device.channel} 信道")
                    if (device.frequencyMhz != 0) {
                        if (isNotEmpty()) append("  ·  ")
                        append("${device.frequencyMhz} MHz")
                    }
                    facts.channelWidth?.let { append("  ·  带宽 $it") }
                },
            )
        }
        facts.wifiStandard?.let { line("Wi-Fi 协议代际", it) }
        if (facts.centerFreq0 != null || facts.centerFreq1 != null) {
            line(
                "中心频率",
                listOfNotNull(
                    facts.centerFreq0?.let { "$it MHz" },
                    facts.centerFreq1?.let { "$it MHz" },
                ).joinToString("  ·  "),
            )
        }
        val rssiTail = device.rssiHistory.filter { Rssi.measured(it.rssi) }.takeLast(24)
        if (rssiTail.isNotEmpty()) {
            line(
                "近期 RSSI (从早到晚)",
                rssiTail.joinToString(", ") { it.rssi.toString() },
            )
        }

        if (device.kind == RadioKind.BLE) {
            section("低功耗蓝牙广播")
            facts.primaryPhy?.let {
                val phys = listOfNotNull(it, facts.secondaryPhy).distinct()
                line("无线电物理层 (PHY)", phys.joinToString(" / ") { phy -> DeviceExplain.phyExplain(phy) })
            }
            facts.connectable?.let {
                line(
                    "可连接性",
                    if (it) "是 — 手机可与其建立 BLE 连接"
                    else "否 — 仅限单向广播 (仅可被动侦听，无法连接)",
                )
            }
            facts.advertisingIntervalMs?.let {
                line("广播间隔周期", "广播脉冲间隔 %.0f 毫秒 (数值越小代表空中发包越密集)".format(it))
            }
            facts.periodicIntervalMs?.let { line("周期性广播", "%.0f 毫秒".format(it)) }
            facts.advFlags?.let { flags ->
                line("可发现模式", DeviceExplain.flagsExplain(flags))
                line("标志位 (原始值)", "0x%02X".format(flags))
            }
            facts.appearance?.let { value ->
                val name = RadioDb.appearance(value)
                line(
                    "设备类型声明 (Appearance)",
                    name ?: "未收录的类型声明 0x%04X".format(value),
                )
                line("类型声明编码", "0x%04X".format(value))
            }
            CodDecoder.decodeOrNull(facts.deviceClass)?.let { cod ->
                line(
                    "经典蓝牙设备类别 (CoD)",
                    buildString {
                        append(cod.major)
                        if (cod.minor.isNotBlank()) append(" / ").append(cod.minor)
                        if (cod.services.isNotEmpty()) {
                            append("。同时提供服务: ")
                            append(cod.services.joinToString(", "))
                        }
                    },
                )
            }
        }

        if (device.kind == RadioKind.WIFI) {
            section("Wi-Fi 接入点")
            facts.security?.let {
                line("加密 / 认证模式", DeviceExplain.wifiSecurityExplain(it))
                if (it.isNotBlank()) line("安全配置特征串", it)
            }
            facts.supportedRates?.let { line("支持速率", "$it Mbps  (* = 必需基础速率)") }
            facts.capabilities?.takeIf { it.isNotBlank() && it != facts.security }?.let {
                line("能力特征串", it)
            }
        }

        if (fleets.isNotEmpty() && (device.kind == RadioKind.BLE || device.kind == RadioKind.WIFI)) {
            val decoded = SignatureFieldDecoder.decodeSighting(device, fleets)
            if (decoded.isNotEmpty()) {
                section("已解码字段")
                decoded.forEach { row ->
                    line(row.label, row.display)
                    if (row.note.isNotBlank()) line("备注", row.note)
                }
            }
        }

        if (device.serviceUuids.isNotEmpty()) {
            section("提供服务 (Service UUID)")
            line(
                "服务 UUID",
                device.serviceUuids.joinToString("; ") { uuid ->
                    DeviceExplain.uuidGloss(uuid)?.let { "$uuid  ·  $it" } ?: uuid
                },
            )
        }
        if (facts.serviceData.isNotEmpty()) {
            facts.serviceData.forEach { sd ->
                val named = RadioDb.serviceUuid(sd.uuid)?.let { " ($it)" } ?: ""
                AdvPayloadDecoder.decodeService(sd).forEach { field -> line(field.label, field.value) }
                line(
                    "Service data ${uuidShort(sd.uuid)}$named",
                    sd.dataHex.hexSpaced().ifBlank { "(empty)" },
                )
            }
        }

        val mfg = facts.mfgRecords.ifEmpty {
            device.manufacturerId?.let {
                listOf(MfgRecord(it, device.manufacturerDataHex))
            } ?: emptyList()
        }
        if (mfg.isNotEmpty()) {
            section("广播中的厂商自定义数据")
            mfg.forEach { rec ->
                val company = RadioDb.company(rec.companyId) ?: "不在标准蓝牙联盟厂商列表中"
                line("蓝牙厂商 0x%04X".format(rec.companyId), company)
                BleAdParser.mfgDecodedFields(rec).forEach { (k, v) -> line(k, v) }
                if (rec.dataHex.isNotBlank()) {
                    line("原始载荷 (${rec.dataHex.length / 2} 字节)", rec.dataHex.hexSpaced())
                }
            }
        }

        if (facts.vendorIes.isNotEmpty() || device.vendorIeOuis.isNotEmpty()) {
            section("Wi-Fi 厂商专有标签 (Vendor IE)")
            val rows = facts.vendorIes.ifEmpty {
                device.vendorIeOuis.map { VendorIeRecord(it, -1, "") }
            }
            rows.forEach { ie ->
                val org = RadioDb.vendorForOui24(ie.oui)
                val type = if (ie.type >= 0) " type %d".format(ie.type) else ""
                line(
                    "厂商 OUI ${ie.oui}$type",
                    buildString {
                        append(org ?: "未知 IEEE OUI")
                        append(" — 接入点额外信息元素，非 SSID。")
                        if (ie.dataHex.isNotBlank()) {
                            append(" ")
                            append(ie.dataHex.hexSpaced())
                        }
                    },
                )
            }
        }

        section("本次监测会话")
        line("首次侦听", fmt.format(Date(device.firstSeen)))
        line("最近侦听", fmt.format(Date(device.lastSeen)))
        line("侦听命中次数", device.hitCount.toString())
        Geo.screenCoord(device.latitude, device.longitude, false)?.let {
            line("最后坐标", it)
            out.append("最后坐标为接收到信号时本机的 GPS 定位，并非该无线电设备自身的位置。\n")
        }
        if (device.fleetIds.isNotEmpty()) {
            line("匹配特征", signatureNames.joinToString("; ").ifBlank {
                device.fleetIds.joinToString("; ")
            })
        }
        if (device.rawHex.isNotBlank() && device.kind == RadioKind.BLE) {
            line("原始广播数据", device.rawHex.hexSpaced())
        }
        presenceLine(device, now, fmt)?.let { line("活跃时间段 (15分钟内)", it) }
        return out.toString().trimEnd() + "\n"
    }

    private fun vendorLine(device: Sighting): String? {
        val parts = ArrayList<String>(3)
        device.vendor?.let {
            parts += "IEEE 主板/芯片厂商: $it (${device.oui})。此项代表 MAC 地址段归属方，并不一定等同于终端产品品牌。"
        }
        val mfgId = device.facts.mfgRecords.firstOrNull()?.companyId ?: device.manufacturerId
        if (mfgId != null) {
            val company = RadioDb.company(mfgId)
            parts += "广播中声明的蓝牙厂商: ${company ?: "未收录"} (0x%04X)。".format(mfgId)
        }
        return parts.joinToString(" ").ifBlank { null }
    }

    private fun uuidShort(uuid: String): String {
        val hex = uuid.filter { it.isLetterOrDigit() }.uppercase()
        return if (hex.length >= 8 && hex.startsWith("0000")) hex.substring(4, 8) else uuid.take(8)
    }

    private fun presenceLine(device: Sighting, now: Long, fmt: SimpleDateFormat): String? {
        if (device.presence.isEmpty()) return null
        val from = now - 15 * 60 * 1000L
        val spans = device.presence.filter { (it.end ?: now) >= from }
        if (spans.isEmpty()) return null
        return spans.joinToString("; ") { span ->
            val start = fmt.format(Date(span.start.coerceAtLeast(from)))
            val end = span.end?.let { fmt.format(Date(it)) } ?: "至今"
            "$start–$end"
        }
    }
}
