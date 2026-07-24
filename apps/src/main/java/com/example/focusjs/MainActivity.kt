package com.example.focusjs

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import androidx.core.view.isVisible
import android.os.Build
import android.view.WindowInsetsController
import java.io.File
import android.net.Uri
import android.provider.Settings
class MainActivity : AppCompatActivity() {
    private var isTerminalFullscreen = false
    private var currentDir: File = Environment.getExternalStorageDirectory()
    private lateinit var statusManager: StatusWidgetManager
    private lateinit var appManager: AppManager
    private lateinit var pinnedAppsManager: PinnedAppsManager
    private lateinit var syntaxHighlighter: SyntaxHighlighter
    private lateinit var codeContainer: LinearLayout

    // Header & Layout Toggle Views
    private lateinit var btnToggleTerminal: ImageView
    private lateinit var editorContainer: LinearLayout
    private lateinit var terminalContainer: LinearLayout
    private lateinit var terminalDivider: View

    // Terminal Views
    private lateinit var terminalOutput: TextView
    private lateinit var terminalInput: EditText
    private lateinit var terminalScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkStoragePermission()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isTerminalFullscreen) {
                    toggleTerminalFullscreen()
                }
                // Do nothing, or hide the terminal if it is currently open
                else if (terminalContainer.isVisible) {
                    toggleTerminal()
                }
                // If terminal is already closed, doing nothing keeps user on home screen
            }
        })

        statusManager = StatusWidgetManager(this)
        appManager = AppManager(this)
        pinnedAppsManager = PinnedAppsManager(this)
        syntaxHighlighter = SyntaxHighlighter(this)
        codeContainer = findViewById(R.id.codeContainer)

        // Bind layout toggle views
        btnToggleTerminal = findViewById(R.id.btnToggleTerminal)
        editorContainer = findViewById(R.id.editorContainer)
        terminalContainer = findViewById(R.id.terminalContainer)
        terminalDivider = findViewById(R.id.terminalDivider)

        // Toggle terminal on icon click
        btnToggleTerminal.setOnClickListener {
            toggleTerminal()
        }

        // Pre-load apps in background
        lifecycleScope.launch(Dispatchers.IO) {
            appManager.getInstalledApps()
        }

        // Bind terminal views
        terminalOutput = findViewById(R.id.terminalOutput)
        terminalInput = findViewById(R.id.terminalInput)
        terminalScrollView = findViewById(R.id.terminalScrollView)

        terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                val command = terminalInput.text.toString().trim()
                if (command.isNotEmpty()) {
                    executeCommand(command)
                    terminalInput.text.clear()
                }
                true
            } else false
        }

        // Set default 6 pinned apps on first startup
        if (!pinnedAppsManager.isInitialized()) {
            val defaultApps = appManager.getInstalledApps().take(6).map { it.packageName }
            pinnedAppsManager.saveInitialDefaults(defaultApps)
        }

        renderHomeScreen()
    }

    override fun onResume() {
        super.onResume()
        renderHomeScreen()
    }
    private fun showWelcomeMessage() {
        terminalOutput.text = "$ focus.js terminal ready\n$ type 'help' for commands\n"
    }
    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
    private fun toggleTerminal() {
        val editorParams = editorContainer.layoutParams as LinearLayout.LayoutParams

        if (terminalContainer.isVisible) {
            // Hide terminal, ensure editor is visible and takes 100% space
            terminalContainer.visibility = View.GONE
            terminalDivider.visibility = View.GONE
            editorContainer.visibility = View.VISIBLE
            editorParams.weight = 1.0f
            isTerminalFullscreen = false

            // Reset output when closing
            terminalOutput.text = ""
        } else {
            // Show terminal & set standard 75% / 25% split
            editorContainer.visibility = View.VISIBLE
            terminalContainer.visibility = View.VISIBLE
            terminalDivider.visibility = View.VISIBLE
            editorParams.weight = 0.75f

            // Display welcome message on every open
            showWelcomeMessage()
        }

        editorContainer.layoutParams = editorParams
    }
    private fun toggleTerminalFullscreen() {
        isTerminalFullscreen = !isTerminalFullscreen

        val editorParams = editorContainer.layoutParams as LinearLayout.LayoutParams
        val terminalParams = terminalContainer.layoutParams as LinearLayout.LayoutParams

        if (isTerminalFullscreen) {
            // Hide editor and make terminal fill 100%
            editorContainer.visibility = View.GONE
            terminalParams.weight = 1.0f
            appendOutput("Terminal expanded to 100%")
        } else {
            // Restore to normal split view
            editorContainer.visibility = View.VISIBLE
            editorParams.weight = 0.75f
            terminalParams.weight = 0.25f
            appendOutput("Terminal restored to normal size")
        }

        editorContainer.layoutParams = editorParams
        terminalContainer.layoutParams = terminalParams
    }

    private fun renderHomeScreen() {
        codeContainer.removeAllViews()

        var lineNumber = 1
        val status = statusManager.getLiveStatus()
        val allApps = appManager.getInstalledApps()

        val pinnedPackageNames = pinnedAppsManager.getPinnedPackageNames()
        val homeApps = allApps.filter { pinnedPackageNames.contains(it.packageName) }

        // Line 1: {
        addLine(lineNumber++, syntaxHighlighter.color("{", R.color.punctuation))

        // Line 2:   "status_widget": {
        val statusKey = SpannableStringBuilder()
            .append("  ")
            .append(syntaxHighlighter.color("\"status_widget\"", R.color.json_key))
            .append(syntaxHighlighter.color(": {", R.color.punctuation))
        addLine(lineNumber++, statusKey)

        // Status lines
        addLine(lineNumber++, formatJsonKeyValue("time", status.time, comma = true))
        addLine(lineNumber++, formatJsonKeyValue("date", status.date, comma = true))
        addLine(lineNumber++, formatJsonKeyValue("battery", status.battery, comma = false))

        // Line 6:   },
        val statusClose = SpannableStringBuilder()
            .append("  ")
            .append(syntaxHighlighter.color("},", R.color.punctuation))
        addLine(lineNumber++, statusClose)

        // Line 7:   "apps": [
        val appsKey = SpannableStringBuilder()
            .append("  ")
            .append(syntaxHighlighter.color("\"apps\"", R.color.json_key))
            .append(syntaxHighlighter.color(": [", R.color.punctuation))
        addLine(lineNumber++, appsKey)

        // App Lines
        homeApps.forEachIndexed { index, app ->
            val isLast = index == homeApps.size - 1
            val appLine = SpannableStringBuilder().append("    ")

            val clickable = syntaxHighlighter.clickableAppString(
                appName = app.label,
                colorRes = R.color.app_string,
                onClick = { appManager.launchApp(app.packageName) },
                onLongClick = { showUnpinDialog(app) }
            )
            appLine.append(clickable)

            if (!isLast) {
                appLine.append(syntaxHighlighter.color(",", R.color.punctuation))
            }
            addLine(lineNumber++, appLine)
        }

        // Line:   ],
        val appsClose = SpannableStringBuilder()
            .append("  ")
            .append(syntaxHighlighter.color("],", R.color.punctuation))
        addLine(lineNumber++, appsClose)

        // Line:   "action": "view_all_apps()"
        val actionLine = SpannableStringBuilder()
            .append("  ")
            .append(syntaxHighlighter.color("\"action\"", R.color.json_key))
            .append(syntaxHighlighter.color(": ", R.color.punctuation))

        val actionClickable = syntaxHighlighter.clickableAppString(
            appName = "view_all_apps()",
            colorRes = R.color.js_keyword,
            onClick = { openAllAppsScreen() }
        )
        actionLine.append(actionClickable)
        addLine(lineNumber++, actionLine)

        // Line: }
        addLine(lineNumber++, syntaxHighlighter.color("}", R.color.punctuation))
    }

    private fun showUnpinDialog(app: AppManager.AppInfo) {
        AlertDialog.Builder(this)
            .setTitle("Remove App")
            .setMessage("Remove \"${app.label}\" from home screen?")
            .setPositiveButton("Remove") { _, _ ->
                pinnedAppsManager.unpinApp(app.packageName)
                renderHomeScreen()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatJsonKeyValue(
        key: String,
        value: String,
        comma: Boolean,
        indentLevel: Int = 2
    ): SpannableStringBuilder {
        val indent = "  ".repeat(indentLevel)
        val ssb = SpannableStringBuilder()
            .append(indent)
            .append(syntaxHighlighter.color("\"$key\"", R.color.json_key))
            .append(syntaxHighlighter.color(": ", R.color.punctuation))
            .append(syntaxHighlighter.color("\"$value\"", R.color.app_string))

        if (comma) {
            ssb.append(syntaxHighlighter.color(",", R.color.punctuation))
        }
        return ssb
    }

    private fun addLine(lineNum: Int, lineContent: CharSequence) {
        val lineLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
        }

        val font = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val numView = TextView(this).apply {
            text = String.format(Locale.getDefault(), "%2d ", lineNum)
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.line_number))
            textSize = 14f
            typeface = font
        }

        val contentView = TextView(this).apply {
            text = lineContent
            textSize = 14f
            typeface = font
            highlightColor = android.graphics.Color.TRANSPARENT
        }

        if (lineContent is SpannableStringBuilder) {
            setupTouchListener(contentView, lineContent)
        }

        lineLayout.addView(numView)
        lineLayout.addView(contentView)
        codeContainer.addView(lineLayout)
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

        textView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                view.performClick()
            }
            gestureDetector.onTouchEvent(event)
            true
        }
        textView.text = content
    }

    private fun openAllAppsScreen() {
        val intent = Intent(this, AllAppsActivity::class.java)
        startActivity(intent)
    }

    // --- TERMINAL EXECUTOR ---

    private fun executeCommand(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return

        appendOutput("$ $trimmed")

        val parts = trimmed.split("\\s+".toRegex())
        val command = parts[0].lowercase(Locale.getDefault())
        val args = parts.drop(1)

        when (command) {
            "clear" -> terminalOutput.text = ""
            "fullscreen", "toggle-fullscreen" -> toggleTerminalFullscreen()
            "pin", "unpin" -> handleAppPinningCommands(command, args)
            "help" -> appendOutput(
                "Commands:\n" +
                        "  cd / ls / cp / mv / rm (standard shell commands)" +
                        "  open <app_name>\n" +
                        "  pin <app_name>\n" +
                        "  unpin <app_name>\n" +
                        "  fullscreen\n" +
                        "  clear"
            )
            "open" -> {
                if (args.isEmpty()) {
                    appendOutput("Error: specify app name (e.g. 'open YouTube')")
                } else {
                    val query = args.joinToString(" ")
                    val installedApps = appManager.getInstalledApps()
                    val targetApp = installedApps.find { it.label.equals(query, ignoreCase = true) }
                        ?: installedApps.find { it.label.contains(query, ignoreCase = true) }

                    if (targetApp != null) {
                        appendOutput("Opening ${targetApp.label}...")
                        appManager.launchApp(targetApp.packageName)
                    } else {
                        appendOutput("App '$query' not found.")
                    }
                }
            }
            else -> runShellCommand(trimmed)
        }
    }
    private fun handleAppPinningCommands(command: String, args: List<String>) {
        if (args.isEmpty()) {
            appendOutput("Error: specify app name (e.g., '$command WhatsApp')")
            return
        }

        val query = args.joinToString(" ")
        val installedApps = appManager.getInstalledApps()
        val targetApp = installedApps.find { it.label.equals(query, ignoreCase = true) }
            ?: installedApps.find { it.label.contains(query, ignoreCase = true) }

        if (targetApp == null) {
            appendOutput("App '$query' not found.")
            return
        }

        val currentPinned = pinnedAppsManager.getPinnedPackageNames()

        when (command) {
            "pin" -> {
                if (currentPinned.contains(targetApp.packageName)) {
                    appendOutput("'${targetApp.label}' is already pinned.")
                } else {
                    pinnedAppsManager.pinApp(targetApp.packageName)
                    renderHomeScreen()
                    appendOutput("Pinned '${targetApp.label}' to home screen.")
                }
            }
            "unpin" -> {
                if (!currentPinned.contains(targetApp.packageName)) {
                    appendOutput("'${targetApp.label}' is not pinned.")
                } else {
                    pinnedAppsManager.unpinApp(targetApp.packageName)
                    renderHomeScreen()
                    appendOutput("Unpinned '${targetApp.label}' from home screen.")
                }
            }
        }
    }
    private fun runShellCommand(command: String) {
        val parts = command.split("\\s+".toRegex())
        val cmd = parts[0].lowercase(Locale.getDefault())

        // Direct cd check inside shell runner
        if (cmd == "cd") {
            val targetPath = if (parts.size > 1) parts[1] else "~"
            changeDirectory(targetPath)
            return
        }

        try {
            val processBuilder = ProcessBuilder("sh", "-c", command)
                .directory(currentDir)
                .redirectErrorStream(true)

            processBuilder.environment()["HOME"] = Environment.getExternalStorageDirectory().absolutePath

            val process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            process.waitFor()

            val result = output.toString().trim()
            if (result.isNotEmpty()) {
                appendOutput(result)
            }
        } catch (e: Exception) {
            appendOutput("Execution error: ${e.message}")
        }
    }
    private fun changeDirectory(targetPath: String) {
        val homeDir = Environment.getExternalStorageDirectory()

        val newDir = when {
            targetPath == "~" || targetPath.isEmpty() -> homeDir
            targetPath.startsWith("/") -> File(targetPath) // Absolute path
            else -> File(currentDir, targetPath) // Relative path (e.g. cd Download)
        }

        if (newDir.exists() && newDir.isDirectory) {
            currentDir = newDir.canonicalFile // Resolves ".." cleanly
            appendOutput("Changed directory to: ${currentDir.absolutePath}")
        } else {
            appendOutput("cd: no such file or directory: $targetPath")
        }
    }
    private fun appendOutput(text: String) {
        terminalOutput.append("\n$text")
        terminalScrollView.post {
            terminalScrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
    private var isFullscreen = false

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val controller = window.insetsController
            if (controller != null) {
                if (isFullscreen) {
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    appendOutput("Fullscreen mode: ON")
                } else {
                    controller.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    appendOutput("Fullscreen mode: OFF")
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (isFullscreen) {
                window.decorView.systemUiVisibility = (
                        android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
                appendOutput("Fullscreen mode: ON")
            } else {
                window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
                appendOutput("Fullscreen mode: OFF")
            }
        }
    }


}