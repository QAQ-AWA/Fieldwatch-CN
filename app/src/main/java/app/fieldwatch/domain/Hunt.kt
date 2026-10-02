package app.fieldwatch.domain

/**
 * Relative-loudness hunt for one BLE advertiser. RSSI is not distance
 * and not a bearing. Quiet / gone is as important as closer / further.
 */
enum class HuntCue {
    VERY_CLOSE,
    CLOSER,
    FURTHER,
    SAME,
    WAITING,
    QUIET,
    GONE,
}

object Hunt {
    const val RECENT_MS = 2_000L
    const val EARLIER_FROM_MS = 8_000L
    const val EARLIER_TO_MS = 3_500L
    const val STEP_DB = 3.0
    const val QUIET_MS = 8_000L
    /** Same floor as DeviceExplain “very strong.” Pocket / in-hand / same bag, not meters. */
    const val VERY_CLOSE_DBM = -45.0
    /** Geiger tick: last-heard RSSI mapped to interval. Loud end is faster than Very Close. */
    const val TICK_LOUD_DBM = -40
    const val TICK_QUIET_DBM = -90
    const val TICK_FAST_MS = 90L
    const val TICK_SLOW_MS = 1_400L

    fun cue(
        samples: List<RssiSample>,
        now: Long,
        lastSeen: Long?,
        missing: Boolean,
    ): HuntCue {
        if (missing) return HuntCue.GONE
        if (lastSeen == null) return HuntCue.WAITING
        if (now - lastSeen > QUIET_MS) return HuntCue.QUIET
        val usable = samples.filter { Rssi.measured(it.rssi) }
        val recent = usable.filter { it.at >= now - RECENT_MS }
        val loud = if (recent.isNotEmpty()) {
            recent.map { it.rssi }.average()
        } else {
            usable.lastOrNull { now - it.at <= QUIET_MS }?.rssi?.toDouble()
        }
        if (loud != null && loud >= VERY_CLOSE_DBM) return HuntCue.VERY_CLOSE
        val earlier = usable.filter { it.at in (now - EARLIER_FROM_MS)..(now - EARLIER_TO_MS) }
        if (recent.size < 2 || earlier.size < 2) return HuntCue.WAITING
        val delta = recent.map { it.rssi }.average() - earlier.map { it.rssi }.average()
        return when {
            delta >= STEP_DB -> HuntCue.CLOSER
            delta <= -STEP_DB -> HuntCue.FURTHER
            else -> HuntCue.SAME
        }
    }

    fun label(cue: HuntCue): String = when (cue) {
        HuntCue.VERY_CLOSE -> "极近"
        HuntCue.CLOSER -> "接近中"
        HuntCue.FURTHER -> "远离中"
        HuntCue.SAME -> "距离相仿"
        HuntCue.WAITING -> "监听中…"
        HuntCue.QUIET -> "信号中断"
        HuntCue.GONE -> "已消失"
    }

    fun hint(cue: HuntCue): String = when (cue) {
        HuntCue.VERY_CLOSE -> "信号极强。环顾四周——通常在手中、口袋或同行的包内（而非数米之外）。"
        HuntCue.CLOSER -> "比几秒前信号更强。请朝该方向继续移动。"
        HuntCue.FURTHER -> "比几秒前信号更弱。请调整方向或往回走。"
        HuntCue.SAME -> "信号暂无明显变化。请放慢移动速度，保持手机平稳。"
        HuntCue.WAITING -> "正在收集几秒内的数据包以供对比分析。"
        HuntCue.QUIET -> "数秒内未接收到数据包。设备可能停止发射或被墙体阻隔。"
        HuntCue.GONE -> "已移出实时监听列表。使用随机 MAC 的 BLE 经常会在追踪中途消失。"
    }

    /**
     * Interval between Hunt ticks, or null to stay silent.
     * Quiet / Gone (and no live RSSI) do not tick. Waiting still ticks if a packet is on the screen.
     */
    fun tickIntervalMs(rssi: Int?, cue: HuntCue): Long? {
        if (cue == HuntCue.QUIET || cue == HuntCue.GONE) return null
        val r = rssi ?: return null
        val span = (TICK_LOUD_DBM - TICK_QUIET_DBM).toDouble()
        val t = ((r - TICK_QUIET_DBM) / span).coerceIn(0.0, 1.0)
        return (TICK_SLOW_MS + (TICK_FAST_MS - TICK_SLOW_MS) * t).toLong()
    }
}
