package app.fieldwatch.domain

/** Operator-facing disclaimer. First-run, Debrief, and AI Export share the same core. */
object FieldwatchDisclaimer {
    const val HOBBY =
        "本项目为一个开源业余项目，在 MIT 许可证下按“原样”提供。使用风险自负。"

    const val HYPOTHESES =
        "检测结果、特征模式匹配、“随行移动”/“疑似尾随”判断、研判报告文字以及 AI 导出内容均为推测与假设 — " +
            "不代表设备真实确切身份，不具备法律证据效力，亦非完整的全频段射频捕获。处于关机、休眠、地址随机化、仅限蜂窝网络或被操作系统限制捕获的设备不会出现。"

    const val LIABILITY =
        "您对本应用的使用方式以及遵守当地法律承担全部责任。" +
            "在法律允许的最大范围内，Off Grid Pete LLC 及开发者不对因使用本软件引起的任何间接、偶然、特殊、后果性或惩罚性损害承担责任。"

    const val LOCATION =
        "GPS 位置标记记录的是侦听到信号时本机（手机）的位置，而非对方无线电设备的位置，" +
            "除非解码特征规则中广播了解析出来的经纬度（如无人机远程 ID 位置）。分享研判报告、监测对比、AI 导出、设备详情分享或日志可能会将轨迹数据导出至设备之外。" +
            "当开启 TAK/CoT 发布时，标记将发送至局域网，相关安全责任由操作员自行承担。"

    const val ACCEPT =
        "勾选复选框并继续，即表示您接受上述条款以及 MIT 许可证。"

    const val LICENSE_TITLE = "MIT 许可证 (MIT License)"

    /** Body of LICENSE in the repository, without the title line. */
    const val LICENSE_BODY =
        "Copyright (c) 2026 Off Grid Pete LLC\n" +
            "\n" +
            "Permission is hereby granted, free of charge, to any person obtaining a copy " +
            "of this software and associated documentation files (the \"Software\"), to deal " +
            "in the Software without restriction, including without limitation the rights " +
            "to use, copy, modify, merge, publish, distribute, sublicense, and/or sell " +
            "copies of the Software, and to permit persons to whom the Software is " +
            "furnished to do so, subject to the following conditions:\n" +
            "\n" +
            "The above copyright notice and this permission notice shall be included in all " +
            "copies or substantial portions of the Software.\n" +
            "\n" +
            "THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR " +
            "IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, " +
            "FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE " +
            "AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER " +
            "LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, " +
            "OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE " +
            "SOFTWARE."

    val LICENSE_TEXT = "$LICENSE_TITLE\n\n$LICENSE_BODY"

    val firstRunDisclaimer: String = "$HOBBY\n\n$HYPOTHESES\n\n$LIABILITY"

    val firstRun: String =
        "$firstRunDisclaimer\n\n$LICENSE_TEXT\n\n$ACCEPT"

    /** Debrief text / PDF. Same core as first-run, without the click-through line. */
    fun report(window: DebriefWindow? = null): String {
        val source = if (window?.sitName != null) {
            "指定监测期“${window.sitName}”（在该时间窗口内侦听到的设备；观察时仍适用实时列表上限）。"
        } else {
            "内存中的实时设备集合（最近 15 分钟，上限约 400 台）。"
        }
        return "$HOBBY\n\n$HYPOTHESES\n\n$LIABILITY\n\n$LOCATION\n\n" +
            "本监测研判报告源自$source" +
            "切勿在任何涉及人身或财产安全关键的情况下将其作为依据。"
    }

    fun compare(): String =
        "$HOBBY\n\n$HYPOTHESES\n\n$LIABILITY\n\n$LOCATION\n\n" +
            "本次对比为本机侦听到的两个时间窗口内的无线电设备（类型 + MAC）。" +
            "BLE 随机地址轮换将被记录为新行。切勿在任何涉及人身或财产安全关键的情况下将其作为依据。"

    fun experimentalMarkdown(): String = buildString {
        appendLine("## 免责声明 (请在回答开头重复本声明)")
        appendLine(HOBBY)
        appendLine(HYPOTHESES)
        appendLine(LIABILITY)
        appendLine(LOCATION)
        appendLine("切勿在任何涉及生命安全或法律关键的情况下依赖 Fieldwatch、本数据或您的分析。")
        appendLine("请在回复的开头声明此内容，切勿提供安全建议。")
    }
}
