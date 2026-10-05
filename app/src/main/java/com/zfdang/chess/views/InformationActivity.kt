package com.zfdang.chess.views

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import java.io.IOException
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.zfdang.chess.BuildConfig
import com.zfdang.chess.R
import com.zfdang.chess.engine.EngineAssets
import com.zfdang.chess.utils.WindowInsetsUtil

/** Offline reading pages. External project links open only when the user selects them. */
class InformationActivity : AppCompatActivity() {
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)
        WindowInsetsUtil.apply(this, findViewById(R.id.information_root))
        content = findViewById(R.id.information_content)
        findViewById<View>(R.id.information_back).setOnClickListener { finish() }
        savedInstanceState?.keySet()?.filter { it.startsWith("section:") }?.forEach {
            expandedSections[it.removePrefix("section:")] = savedInstanceState.getBoolean(it)
        }
        val page = intent.getStringExtra(EXTRA_PAGE) ?: HELP
        val title = when (page) { ABOUT -> "关于象棋鱼"; PRIVACY -> "隐私政策"; else -> "使用帮助" }
        findViewById<TextView>(R.id.information_title).text = title
        when (page) { ABOUT -> about(); PRIVACY -> privacy(); else -> help() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    private fun text(value: CharSequence, size: Float = 16f, color: Int = R.color.ui_ink, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(getColor(color))
            setLineSpacing(dp(5).toFloat(), 1f)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
    private fun heading(title: String, subtitle: String) {
        content.addView(text(title, 32f, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
            ViewCompat.setAccessibilityHeading(this, true)
        })
        content.addView(text(subtitle, 15f, R.color.ui_muted).apply {
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(24) }
        })
    }
    private fun card(title: String, body: CharSequence, key: String, collapsible: Boolean = false, initiallyOpen: Boolean = true) {
        val frame = MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = getColor(R.color.ui_line)
            setCardBackgroundColor(getColor(R.color.ui_surface))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(16))
        }
        val label = text(title, 18f, bold = true).apply {
            minHeight = dp(48)
            gravity = android.view.Gravity.CENTER_VERTICAL
            ViewCompat.setAccessibilityHeading(this, true)
        }
        val paragraph = text(body).apply {
            setTextIsSelectable(true)
            setLinkTextColor(getColor(R.color.ui_red))
            movementMethod = LinkMovementMethod.getInstance()
        }
        if (collapsible) {
            val indicator = text("+", 24f, R.color.ui_muted).apply {
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                gravity = android.view.Gravity.CENTER
            }
            val header = LinearLayout(this).apply {
                gravity = android.view.Gravity.CENTER_VERTICAL
                minimumHeight = dp(48)
                isFocusable = true
                contentDescription = title
                ViewCompat.setAccessibilityHeading(this, true)
                val background = android.util.TypedValue()
                theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
                setBackgroundResource(background.resourceId)
                addView(label, LinearLayout.LayoutParams(0, -2, 1f))
                addView(indicator, LinearLayout.LayoutParams(dp(32), -1))
            }
            label.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            column.addView(header)
            fun expanded(open: Boolean) {
                paragraph.visibility = if (open) View.VISIBLE else View.GONE
                indicator.text = if (open) "−" else "+"
                ViewCompat.setStateDescription(header, if (open) "已展开" else "已折叠")
                expandedSections[key] = open
            }
            expanded(expandedSections[key] ?: initiallyOpen)
            header.setOnClickListener { expanded(paragraph.visibility != View.VISIBLE) }
        } else column.addView(label)
        column.addView(paragraph)
        frame.addView(column)
        content.addView(frame)
    }
    private val expandedSections = mutableMapOf<String, Boolean>()

    override fun onSaveInstanceState(outState: Bundle) {
        expandedSections.forEach { (key, value) -> outState.putBoolean("section:$key", value) }
        super.onSaveInstanceState(outState)
    }
    private fun link(title: String, uri: String) {
        content.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = "$title  ↗"
            minHeight = dp(52)
            cornerRadius = dp(16)
            setTextColor(getColor(R.color.ui_red))
            strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.ui_line))
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            setOnClickListener {
                try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri))) }
                catch (_: ActivityNotFoundException) { Toast.makeText(context, "未找到可打开链接的应用", Toast.LENGTH_SHORT).show() }
            }
        })
    }
    private fun help() {
        heading("从第一步开始", "点开一个主题，了解下棋、分析和打谱的方法。")
        val sections = listOf(
            "开始一盘棋" to "进入「对弈」，点击棋子，再点击目标位置落子。可走位置会在棋盘上标出。\n\n「新局」重新开始，「悔棋」回到上一步。右上角「对弈设置」可调整先行方、电脑走棋和搜索方式。",
            "提示与候选着法" to "轮到红方时点击「提示」，引擎会给出候选着法，并在棋盘上画出箭头。点击候选按钮选择着法，也可以直接在棋盘上走自己的棋。\n\n想换一个电脑着法，可展开「引擎与棋局工具」，选择「变着」。",
            "搜索与闪电出着" to "固定深度：达到指定深度后出着，深度越高通常耗时越长。\n\n固定时间：在设定时间内搜索，适合控制每步等待时间。\n\n无限搜索：持续分析，需点击工具区的「闪电出着」结束搜索并取得结果。搜索太慢时，可降低深度或缩短时间。",
            "FEN 局面与评估" to "展开「引擎与棋局工具」，可从 FEN 字符串开局，也可导出当前局面的 FEN，用于分享、复盘或反馈问题。\n\n「评估」分析当前局面，「走势」查看已有评估记录。未评估的局面不会显示为零分。",
            "打开棋谱" to "进入「打谱」，点击打开按钮选择内置 XQF 棋谱。用前进、后退浏览着法；遇到多个分支时，可选择要查看的分支。\n\n点击对弈按钮，可从当前棋谱局面开始对弈。返回后仍可继续浏览棋谱。",
            "引擎设置与版本选择" to "Hash 是引擎搜索缓存。应根据手机内存设置，过大的缓存可能让系统终止引擎。\n\n开启随机走棋后，电脑会在开局阶段从候选着法中选择，使对局有所变化，但可能降低棋力。先行方设置在新局中生效。\n\nARMv8 版本适用于支持的 ARM64 手机；dotprod 版本要求 CPU 支持点积指令。不确定时选择普通 ARMv8 版本。",
            "反馈问题" to "请附上复现步骤、手机型号、应用版本；棋局相关问题还可附上导出的 FEN。下方链接可在浏览器中打开项目的问题反馈页。"
        )
        sections.forEachIndexed { index, (title, body) -> card(title, body, "help-$index", true, index == 0) }
        link("反馈问题", "$PROJECT/issues")
    }
    private fun about() {
        heading("象棋鱼", "CHESS FISH\n开源免费的中国象棋学习工具")
        card("一局棋，慢慢来。", "与皮卡鱼引擎过招，查看候选着法与局势评估，或打开一份棋谱重温好棋。", "intro")
        card("版本信息", "应用版本  ${BuildConfig.VERSION_NAME} · ${BuildConfig.VERSION_CODE}\n皮卡鱼引擎与 NNUE  ${EngineAssets.VERSION}", "version")
        card("开源与致谢", "由 zfdang 维护。\n\n象棋引擎来自 Pikafish；引擎通信代码基于 DroidFish。感谢开源项目与贡献者。源代码及相关许可可在项目仓库中查看。", "credits")
        link("项目主页", "https://fish.zfdang.com/")
        link("源代码与许可", PROJECT)
        link("版本更新", "$PROJECT/releases")
    }
    private fun privacy() {
        // The same document is published on the website and bundled by the build for offline use.
        val html = try {
            assets.open("documents/privacy.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (error: IOException) {
            Log.w("InformationActivity", "Cannot read bundled privacy policy", error)
            heading("您的数据与隐私", "隐私政策")
            card("暂时无法显示", "本地隐私政策暂时无法读取。您可以查看官网最新政策，或更新应用后重试。", "privacy-unavailable")
            link("查看官网隐私政策", "https://fish.zfdang.com/privacy.html")
            return
        }
        val updated = Regex("更新日期[：:]\\s*([^<]+)").find(html)?.groupValues?.get(1)?.trim().orEmpty()
        val date = if (updated.isNotEmpty()) " · 更新于 $updated" else ""
        heading("您的数据与隐私", "隐私政策$date\n此页面可离线阅读。")
        val article = Regex("<section\\b[^>]*>(.*?)</section>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(html)?.groupValues?.get(1) ?: html
        val titles = Regex("<h4\\b[^>]*>(.*?)</h4>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)).findAll(article).toList()
        if (titles.isEmpty()) {
            card("政策正文", HtmlCompat.fromHtml(article, HtmlCompat.FROM_HTML_MODE_LEGACY).trim(), "privacy-full")
            return
        }
        titles.forEachIndexed { index, match ->
            val end = titles.getOrNull(index + 1)?.range?.first ?: article.length
            val body = article.substring(match.range.last + 1, end)
            card(HtmlCompat.fromHtml(match.groupValues[1], HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                HtmlCompat.fromHtml(body, HtmlCompat.FROM_HTML_MODE_LEGACY).trim(), "privacy-$index")
        }
    }
    companion object {
        const val EXTRA_PAGE = "page"
        const val HELP = "help"
        const val ABOUT = "about"
        const val PRIVACY = "privacy"
        private const val PROJECT = "https://github.com/zfdang/chinese-chess-fish-android"
    }
}
