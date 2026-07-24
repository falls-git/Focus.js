package com.example.focusjs

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class AllAppsActivity : AppCompatActivity() {

    private lateinit var appManager: AppManager
    private lateinit var pinnedAppsManager: PinnedAppsManager
    private lateinit var syntaxHighlighter: SyntaxHighlighter
    private lateinit var container: LinearLayout
    private lateinit var searchInput: EditText

    // Cache the installed apps list so we don't query the system continuously
    private var cachedAppList: List<AppManager.AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Disable default activity window transition delays
        overridePendingTransition(0, 0)
        setContentView(R.layout.activity_all_apps)

        appManager = AppManager(this)
        pinnedAppsManager = PinnedAppsManager(this)
        syntaxHighlighter = SyntaxHighlighter(this)
        container = findViewById(R.id.allAppsCodeContainer)
        searchInput = findViewById(R.id.appSearchInput)
        cachedAppList = appManager.getInstalledApps()
        renderCategorizedApps("")

        searchInput.doOnTextChanged { text, _, _, _ ->
            renderCategorizedApps(text.toString())
        }

        // Fetch installed apps asynchronously on a background thread
        lifecycleScope.launch(Dispatchers.IO) {
            cachedAppList = appManager.getInstalledApps()

            // Render on the Main/UI thread once loaded
            withContext(Dispatchers.Main) {
                renderCategorizedApps("")
            }
        }

        // Search text listener
        searchInput.doOnTextChanged { text, _, _, _ ->
            renderCategorizedApps(text.toString())
        }
    }

    override fun finish() {
        super.finish()
        // Disable exit transition lag as well
        overridePendingTransition(0, 0)
    }

    private fun renderCategorizedApps(query: String) {
        container.removeAllViews()
        var lineNum = 1

        val cleanQuery = query.trim().lowercase(Locale.getDefault())

        // Filter using the cached app list (Fast!)
        val filteredApps = cachedAppList.filter {
            it.label.lowercase(Locale.getDefault()).contains(cleanQuery)
        }

        val groupedApps = filteredApps.groupBy { app ->
            val firstChar = app.label.firstOrNull()?.lowercaseChar() ?: '#'
            if (firstChar.isLetter()) firstChar.toString() else "#"
        }.toSortedMap()

        val comment = syntaxHighlighter.color(
            if (cleanQuery.isEmpty()) "// Focus.js - All Installed Applications"
            else "// Results for: \"$cleanQuery\"",
            R.color.comment
        )
        addLine(lineNum++, comment)

        val exportLine = SpannableStringBuilder()
            .append(syntaxHighlighter.color("export ", R.color.js_keyword))
            .append(syntaxHighlighter.color("const ", R.color.js_keyword))
            .append(syntaxHighlighter.color("apps ", R.color.json_key))
            .append(syntaxHighlighter.color("= {", R.color.punctuation))
        addLine(lineNum++, exportLine)

        val totalGroups = groupedApps.size
        var currentGroupIndex = 0

        for ((groupKey, appsInGroup) in groupedApps) {
            currentGroupIndex++
            val isLastGroup = currentGroupIndex == totalGroups

            val groupHeader = SpannableStringBuilder()
                .append("  ")
                .append(syntaxHighlighter.color("\"$groupKey\"", R.color.json_key))
                .append(syntaxHighlighter.color(": [", R.color.punctuation))
            addLine(lineNum++, groupHeader)

            appsInGroup.forEachIndexed { index, app ->
                val isLastAppInGroup = index == appsInGroup.size - 1
                val appLine = SpannableStringBuilder().append("    ")

                val clickable = syntaxHighlighter.clickableAppString(
                    appName = app.label,
                    colorRes = R.color.app_string,
                    onClick = { appManager.launchApp(app.packageName) },
                    onLongClick = {
                        pinnedAppsManager.pinApp(app.packageName)
                        Toast.makeText(this, "Pinned \"${app.label}\" to Home", Toast.LENGTH_SHORT).show()
                    }
                )
                appLine.append(clickable)

                if (!isLastAppInGroup) {
                    appLine.append(syntaxHighlighter.color(",", R.color.punctuation))
                }
                addLine(lineNum++, appLine)
            }

            val groupClose = SpannableStringBuilder().append("  ")
                .append(syntaxHighlighter.color("]", R.color.punctuation))

            if (!isLastGroup) {
                groupClose.append(syntaxHighlighter.color(",", R.color.punctuation))
            }
            addLine(lineNum++, groupClose)
        }

        val closingLine = syntaxHighlighter.color("};", R.color.punctuation)
        addLine(lineNum++, closingLine)
    }

    private fun addLine(lineNum: Int, lineContent: CharSequence) {
        val lineLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }

        val font = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val numView = TextView(this).apply {
            text = String.format(Locale.getDefault(), "%3d ", lineNum)
            setTextColor(ContextCompat.getColor(this@AllAppsActivity, R.color.line_number))
            textSize = 13f
            typeface = font
        }

        val contentView = TextView(this).apply {
            text = lineContent
            textSize = 13f
            typeface = font
            highlightColor = android.graphics.Color.TRANSPARENT
        }

        setupTouchListener(contentView, lineContent)

        lineLayout.addView(numView)
        lineLayout.addView(contentView)
        container.addView(lineLayout)
    }

    private fun setupTouchListener(textView: TextView, content: CharSequence) {
        if (content !is SpannableStringBuilder) {
            textView.text = content
            return
        }

        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val clickableSpans = content.getSpans(0, content.length, ClickableSpan::class.java)
                if (clickableSpans.isNotEmpty()) {
                    clickableSpans[0].onClick(textView)
                    return true
                }
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                val longSpans = content.getSpans(0, content.length, SyntaxHighlighter.LongClickSpan::class.java)
                if (longSpans.isNotEmpty()) {
                    longSpans[0].onLongClick()
                }
            }
        })

        textView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
        textView.text = content
    }
}