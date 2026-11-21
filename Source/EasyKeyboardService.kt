package com.easykeyboard.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.SeekBar
import android.app.AlertDialog
import android.inputmethodservice.InputMethodService

class EasyKeyboardService : InputMethodService() {
    private var persian = true
    private var caps = false
    private var symbolLayout = false
    private var numberRowVisible = false
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val repeatTasks = mutableMapOf<View, Runnable>()
    private var keyboardScale = 1.0f
    private var buttonColor = Color.rgb(247, 249, 252)
    private var buttonTextColor = Color.rgb(25, 35, 75)

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val appearance = getSharedPreferences("appearance", Context.MODE_PRIVATE)
        keyboardScale = appearance.getFloat("scale", 1.0f).coerceIn(0.75f, 1.25f)
        buttonColor = appearance.getInt("button_color", Color.rgb(247, 249, 252))
        buttonTextColor = appearance.getInt("text_color", Color.rgb(25, 35, 75))
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
            setBackgroundColor(Color.rgb(190, 198, 208))
        }

        // Personal suggestion strip: tap to insert, long-press to remove.
        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val suggestionPrefs = getSharedPreferences("suggestions", Context.MODE_PRIVATE)
        val stored = suggestionPrefs.getStringSet("items", emptySet())?.toList().orEmpty()
        val beforeCursor = currentInputConnection?.getTextBeforeCursor(80, 0)?.toString().orEmpty()
        val currentToken = beforeCursor.substringAfterLast(' ').substringAfterLast('\n')
        val dictionary = listOf(
            "نه", "بله", "چرا", "این", "در", "که", "زیرا", "و", "بلکه", "همه", "یک", "اینجا", "موارد",
            "من", "تو", "ما", "شما", "آنها", "برای", "با", "از", "به", "اما", "اگر", "چون", "بسیار",
            "سلام", "خوب", "ممنون", "لطفاً", "می‌شود", "هست", "نیست", "دارم", "دارید"
        )
        val dynamicDefaults = if (currentToken.isBlank()) emptyList()
            else dictionary.filter { it.startsWith(currentToken) && it != currentToken }.take(6)
        val saved = (dynamicDefaults + stored.filter { currentToken.isBlank() || it.startsWith(currentToken) }).distinct()
        val suggestionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(suggestions)
        }
        saved.forEach { word ->
            val suggestionButton = key(word, false) { typeText(word) }
            suggestionButton.setOnLongClickListener {
                removeSuggestion(word)
                true
            }
            suggestionButton.minWidth = dp(52)
            suggestionButton.maxWidth = dp(260)
            suggestions.addView(suggestionButton, LinearLayout.LayoutParams(-2, dp(34)))
        }
        outer.addView(suggestionScroll, LinearLayout.LayoutParams(-1, dp(38)))

        // Additional action row above the main toolbar
        val actionRowScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf("Select", "Setup", "Save Suggestion").forEach { label ->
            val action = when (label) {
                "Save Suggestion" -> ({ saveSuggestion() })
                "Select" -> ({ selectText() })
                else -> ({ showSetup() })
            }
            val button = key(label, true, action)
            val width = when (label) { "Save Suggestion" -> dp(112); else -> dp(76) }
            actionRow.addView(button, LinearLayout.LayoutParams(width, dp(48)))
        }
        actionRowScroll.addView(actionRow)
        outer.addView(actionRowScroll, LinearLayout.LayoutParams(-1, dp(48)))

        val toolbarScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val toolbarKeys = listOf("«", "»", "−", "_", "Undo", "Redo", "Past", "Copy", "Emoji", "اعراب", "Symbols", "File", "Answer")
        toolbarKeys.forEach { label ->
            val b = key(label, true) {
                when (label) {
                    "Undo" -> undo()
                    "Redo" -> redo()
                    "Past" -> pasteFromClipboard()
                    "Copy" -> copySelection()
                    "Save Suggestion" -> saveSuggestion()
                    "Export List" -> exportSuggestions()
                    "Emoji" -> showGrid("Emoji", listOf("😀","😁","😂","🤣","😊","😍","😘","😎","😭","😡","👍","👎","👏","🙏","❤️","💙","🔥","🌹","🎉","✅","❌","⭐","☀️","🌙"), 6)
                    "اعراب" -> showGrid("اعراب", listOf("َ","ِ","ُ","ً","ٍ","ٌ","ْ","ّ","ٰ","ٓ","ٔ","ٕ"), 6)
                    "Symbols" -> showSymbols()
                    "File" -> {
                        val intent = android.content.Intent(this, FilePickerActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    "Answer" -> answerCurrentEditor()
                    "−" -> typeText("−")
                    "_" -> typeText("_")
                    else -> typeText(label)
                }
            }
            if (label == "−") b.setOnLongClickListener {
                showChoices(b, listOf("−", "–", "—", "ـ")); true
            }
            if (label == "_") b.setOnLongClickListener {
                showChoices(b, listOf("_", "=")); true
            }
            toolbar.addView(b, LinearLayout.LayoutParams(
                if (label == "Answer") dp(82) else dp(if (label.length > 8) 92 else if (label.length > 4) 64 else 48),
                -1
            ))
        }
        toolbarScroll.addView(toolbar)
        outer.addView(toolbarScroll, LinearLayout.LayoutParams(-1, dp(48)))

        if (numberRowVisible) {
            val numberRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val digits = if (persian) listOf("۱","۲","۳","۴","۵","۶","۷","۸","۹","۰")
                         else listOf("1","2","3","4","5","6","7","8","9","0")
            val legends = listOf("@", "( )", "& *", "+", "; =", "%", "{ }", "[ ]", "× ✓", "| ~ #")
            digits.forEachIndexed { index, digit ->
                val keyView = keyWithCornerSymbol(digit, legends[index]) { typeText(digit) }
                numberRow.addView(keyView, LinearLayout.LayoutParams(0, dp(44), 1f))
            }
            outer.addView(numberRow, LinearLayout.LayoutParams(-1, dp(44)))
        }

        val normalRows = if (persian) listOf(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
            listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
            listOf("ظ", "ط", "ژ", "ز", "ر", "ذ", "د", "پ", "و", "چ")
        ) else listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M")
        )
        val alternateRows = listOf(
            listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"),
            listOf("-", "_", "+", "=", "[", "]", "{", "}", ";", "'"),
            listOf(":", "\"", "/", "?", "\\", "|", "<", ">", "×", "✓")
        )
        val rows = if (symbolLayout) alternateRows else normalRows

        rows.forEachIndexed { index, chars ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (index == 2) {
                val shift = key(if (symbolLayout) "ABC" else "Shift", true) {}
                var holdTask: Runnable? = null
                var longPressed = false
                shift.setOnTouchListener { _, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            longPressed = false
                            val task = Runnable { longPressed = true }
                            holdTask = task
                            handler.postDelayed(task, 500)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            holdTask?.let { handler.removeCallbacks(it) }
                            if (longPressed) {
                                caps = !caps
                                symbolLayout = false
                            } else {
                                symbolLayout = !symbolLayout
                            }       
                            
                            setInputView(buildKeyboard())
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            holdTask?.let { handler.removeCallbacks(it) }
                            true
                        }
                        else -> true
                    }
                }
                row.addView(shift, LinearLayout.LayoutParams(0, -1, 1.35f))
            }
            chars.forEach { ch ->
                val shown = if (!symbolLayout && !persian && caps) ch.uppercase()
                            else if (!symbolLayout && !persian) ch.lowercase() else ch
                val letterKey = key(shown, false) {
                    typeText(shown)
                    if (symbolLayout) {
                        symbolLayout = false
                        setInputView(buildKeyboard())
                    }
                }
                val variants = if (!symbolLayout && persian) when (ch) {
                    "ا" -> listOf("آ", "أ", "إ")
                    "و" -> listOf("ؤ")
                    "ی" -> listOf("ئ", "ي", "ى")
                    else -> emptyList()
                } else emptyList()
                if (variants.isNotEmpty()) letterKey.setOnLongClickListener {
                    showChoices(letterKey, variants); true
                }
                row.addView(letterKey, LinearLayout.LayoutParams(0, -1, 1f))
                if (!symbolLayout && !persian && index == 2 && ch == "M") {
                    row.addView(key(",", true) { typeText(",") },
                        LinearLayout.LayoutParams(0, -1, 1f))
                }
            }
            if (index == 1) {
                val back = key("← Backspace", true) { deleteOne() }
                back.contentDescription = "Backspace"
                enableRepeat(back) { deleteOne() }
                row.addView(back, LinearLayout.LayoutParams(0, -1, 1.8f))
            }
            if (index == 2) row.addView(key("Enter", true) { answerCurrentEditor() },
                LinearLayout.LayoutParams(0, -1, 1.8f))
            outer.addView(row, LinearLayout.LayoutParams(-1, (dp(61) * keyboardScale).toInt()))
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val screenWidthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val numberKeyUnit = ((screenWidthDp - 4f) / 10f).coerceAtLeast(dp(18).toFloat())
        val sameAsNumberKey = numberKeyUnit.toInt()
        fun addBottom(label: String, action: () -> Unit, corner: String? = null) {
            val buttonView = if (corner == null) key(label, true, action)
                             else keyWithCornerSymbol(label, corner, action)
            bottom.addView(buttonView, LinearLayout.LayoutParams(sameAsNumberKey, -1))
        }
        addBottom("123", {
            numberRowVisible = !numberRowVisible
            setInputView(buildKeyboard())
        })
        addBottom(if (persian) "EN" else "FA", {
            persian = !persian
            caps = false
            symbolLayout = false
            setInputView(buildKeyboard())
        })
        val space = key("Space", true) { typeText(" ") }
        bottom.addView(space, LinearLayout.LayoutParams((numberKeyUnit * 2.4f).toInt(), -1))
        addBottom(".", { typeText(".") }, ";")
        val circle = key("○", true) { typeText("○") }
        bottom.addView(circle, LinearLayout.LayoutParams((sameAsNumberKey * 1.18f).toInt(), -1))
        addBottom("؟", { typeText("؟") }, "✓")
        addBottom("،", { typeText("،") })

        listOf(
            Triple("←", KeyEvent.KEYCODE_DPAD_LEFT, "|"),
            Triple("→", KeyEvent.KEYCODE_DPAD_RIGHT, "~"),
            Triple("↑", KeyEvent.KEYCODE_DPAD_UP, "#"),
            Triple("↓", KeyEvent.KEYCODE_DPAD_DOWN, null)
        ).forEach { (label, code, corner) ->
            val b = if (corner == null) key(label, true) { sendArrow(code) }
                    else keyWithCornerSymbol(label, corner) { sendArrow(code) }
            enableRepeat(b) { sendArrow(code) }
            val arrowWidth = if (label == "←" || label == "→") (sameAsNumberKey * 1.22f).toInt() else sameAsNumberKey
            bottom.addView(b, LinearLayout.LayoutParams(arrowWidth, -1))
        }
        val bottomScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            isFillViewport = true
            addView(bottom, ViewGroup.LayoutParams(-2, -1))
        }
        outer.addView(bottomScroll, LinearLayout.LayoutParams(-1, (dp(58) * keyboardScale).toInt()))
return outer
    }

    private fun key(label: String, dark: Boolean, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = (when {
                label.length >= 8 -> 9f
                label.length >= 5 -> 11f
                label.length >= 4 -> 12f
                else -> 18f
            } * keyboardScale).coerceAtLeast(8f)
            setTextColor(when {
                label.contains("Backspace") -> Color.rgb(105, 25, 25)
                label == "123" || label.all { it.isDigit() } -> Color.rgb(130, 75, 35)
                else -> buttonTextColor
            })
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = 0; minimumWidth = 0; minHeight = 0; minimumHeight = 0
            setPadding(0, 0, 0, 0)
            isSoundEffectsEnabled = false
            fun bg(color: Int) = GradientDrawable().apply {
                setColor(color); cornerRadius = dp(7).toFloat()
                setStroke(dp(1), Color.rgb(105, 116, 130))
            }
            val normal = when (label) {
                "← Backspace" -> Color.rgb(244, 185, 185)
                "Enter" -> Color.rgb(190, 220, 250)
                else -> buttonColor
            }
            background = bg(normal)
            setOnTouchListener { v, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.background = bg(Color.rgb(255, 220, 35))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.background = bg(normal)
                }
                false
            }
            setOnClickListener { action() }
        }

    private fun keyWithCornerSymbol(label: String, corner: String, action: () -> Unit): View {
        val frame = FrameLayout(this)
        val mainButton = key(label, false, action)
        frame.addView(mainButton, FrameLayout.LayoutParams(-1, -1))
        val mark = TextView(this).apply {
            text = corner
            textSize = 8f
            setTextColor(Color.rgb(90, 35, 35))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(3), dp(1), dp(1), 0)
            isClickable = false
            isFocusable = false
        }
        frame.addView(mark, FrameLayout.LayoutParams(-1, -1))
        return frame
    }


    private fun selectText() {
        val ic = currentInputConnection
        if (ic == null) {
            Toast.makeText(this, "ویرایشگری در دسترس نیست", Toast.LENGTH_SHORT).show()
            return
        }
        val selected = ic.performContextMenuAction(android.R.id.selectAll)
        if (!selected) {
            Toast.makeText(this, "انتخاب متن در این برنامه پشتیبانی نمی‌شود", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSetup() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(8))
        }
        root.addView(TextView(this).apply {
            text = "اندازه صفحه‌کلید"
            textSize = 16f
        })
        val size = SeekBar(this).apply {
            max = 50
            progress = ((keyboardScale - 0.75f) * 100f).toInt().coerceIn(0, 50)
        }
        root.addView(size, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(TextView(this).apply {
            text = "رنگ دکمه‌ها"
            textSize = 16f
        })
        val colors = listOf(
            "روشن" to Color.rgb(247,249,252),
            "آبی" to Color.rgb(190,220,250),
            "سبز" to Color.rgb(196,232,199),
            "زرد" to Color.rgb(255,238,170),
            "صورتی" to Color.rgb(246,205,215),
            "خاکستری" to Color.rgb(215,220,226)
        )
        val colorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        colors.forEach { (name, color) ->
            val b = Button(this).apply { 
                text = name
                textSize = 10f
                setOnClickListener {
                    buttonColor = color
                    getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putInt("button_color", color).apply()
                    setInputView(buildKeyboard())
                }
            }
            colorRow.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        root.addView(colorRow)
        root.addView(TextView(this).apply {
            text = "رنگ نوشته دکمه‌ها"
            textSize = 16f
        })
        val textColors = listOf(
            "مشکی" to Color.rgb(25,35,75),
            "سفید" to Color.WHITE,
            "قرمز" to Color.rgb(170,35,35),
            "آبی" to Color.rgb(25,75,170),
            "سبز" to Color.rgb(25,110,55)
        )
        val textColorRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        textColors.forEach { (name, color) ->
            val b = Button(this).apply {
                text = name
                textSize = 10f
                setOnClickListener {
                    buttonTextColor = color
                    getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putInt("text_color", color).apply()
                    setInputView(buildKeyboard())
                }
            }
            textColorRow.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        root.addView(textColorRow)
        val export = Button(this).apply {
            text = "Export List"
            setOnClickListener { exportSuggestions() }
        }
        root.addView(export, LinearLayout.LayoutParams(-1, dp(48)))
        AlertDialog.Builder(this)
            .setTitle("Setup")
            .setView(root)
            .setPositiveButton("ذخیره") { _, _ ->
                keyboardScale = 0.75f + size.progress / 100f
                getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().putFloat("scale", keyboardScale).apply()
                setInputView(buildKeyboard())
            }
            .setNegativeButton("بستن", null)
            .show()
    }

    private fun removeSuggestion(word: String) {
        val prefs = getSharedPreferences("suggestions", Context.MODE_PRIVATE)
        val items = prefs.getStringSet("items", emptySet())?.toMutableSet() ?: mutableSetOf()
        val defaults = setOf("نه", "بله", "چرا", "این", "در", "که", "زیرا", "و", "بلکه", "همه", "یک", "اینجا", "موارد")
        if (word in defaults) {
            val hidden = prefs.getStringSet("hidden_defaults", emptySet())?.toMutableSet() ?: mutableSetOf()
            hidden.add(word)
            prefs.edit().putStringSet("hidden_defaults", hidden).apply()
        } else {
            items.remove(word)
            prefs.edit().putStringSet("items", items).apply()
        }
        Toast.makeText(this, "پیشنهاد حذف شد", Toast.LENGTH_SHORT).show()
        setInputView(buildKeyboard())
    }

    private fun exportSuggestions() {
        val intent = android.content.Intent(this, FilePickerActivity::class.java)
            .putExtra("export_suggestions", true)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun enableRepeat(view: View, action: () -> Unit) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    action()
                    val task = object : Runnable {
                        override fun run() { action(); handler.postDelayed(this, 75) }
                    }
                    repeatTasks[v] = task
                    handler.postDelayed(task, 380)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    repeatTasks.remove(v)?.let { handler.removeCallbacks(it) }
                    true
                }
                else -> true
            }
        }
    }

    private fun showChoices(anchor: View, choices: List<String>) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.rgb(190, 198, 208))
        }
        val popup = PopupWindow(content, -2, dp(58), true).apply {
            isOutsideTouchable = true; isFocusable = true; isTouchable = true; elevation = dp(8).toFloat()
        }
        choices.forEach { value ->
            content.addView(key(value, false) { typeText(value); popup.dismiss() },
                LinearLayout.LayoutParams(dp(48), dp(50)))
        }
        popup.showAtLocation(anchor, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(250))
    }

    private fun showSymbols() {
        val symbols = listOf(
            "©","®","™","§","¶","•","…","‰","°","℃",
            "℉","№","₽","$","€","£","¥","¢","₹","₩",
            "+","−","×","÷","=","≠","≈","≡","<",">",
            "≤","≥","±","√","∞","∑","∏","∫","π","%","‰",
            "&","@","#","*","/","\\","|","~","^","_","(",")",";","{","}","[","]","✓",
            "←","→","↑","↓","↔","↕","○","●","□","■"
        )
        showGrid("Symbols", symbols, 6)
    }

    private fun showGrid(title: String, items: List<String>, columns: Int) {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(6), dp(6), dp(6)) }
        root.addView(TextView(this).apply { text = title; setTextColor(Color.DKGRAY); textSize = 14f })
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(content) }
        lateinit var popup: PopupWindow
        items.chunked(columns).forEach { group ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            group.forEach { item ->
                val symbolButton = key(item, false) {
                    typeText(item)
                    popup.dismiss()
                }.apply { isClickable = true; isFocusable = true }
                row.addView(symbolButton, LinearLayout.LayoutParams(0, dp(44), 1f))
            }
            content.addView(row)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        popup = PopupWindow(root, -1, dp(300), true).apply {
            isOutsideTouchable = true
            isFocusable = true
            isTouchable = true
            elevation = dp(8).toFloat()
            setBackgroundDrawable(GradientDrawable().apply {
                setColor(Color.rgb(235, 239, 245))
                cornerRadius = dp(8).toFloat()
            })
        }
        popup.showAtLocation(window.window?.decorView ?: root, Gravity.BOTTOM, 0, dp(220))
    }
    private var popupDismiss: (() -> Unit)? = null

    private fun saveSuggestion() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)?.toString()
        val candidate = if (!selected.isNullOrBlank()) selected else ic.getTextBeforeCursor(120, 0)?.toString()?.trim()?.takeLast(80)
        if (candidate.isNullOrBlank()) {
            Toast.makeText(this, "Select or type a word/sentence first", Toast.LENGTH_SHORT).show()
            return
        }
        val prefs = getSharedPreferences("suggestions", Context.MODE_PRIVATE)
        val items = prefs.getStringSet("items", emptySet())?.toMutableSet() ?: mutableSetOf()
        items.add(candidate)
        prefs.edit().putStringSet("items", items).apply()
        Toast.makeText(this, "Saved to suggestions", Toast.LENGTH_SHORT).show()
        setInputView(buildKeyboard())
    }

    private fun deleteOne() { currentInputConnection?.deleteSurroundingText(1, 0) }

    private fun answerCurrentEditor() {
        val connection = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_UNSPECIFIED
        if (action != EditorInfo.IME_ACTION_UNSPECIFIED && action != EditorInfo.IME_ACTION_NONE) {
            if (connection.performEditorAction(action)) return
        }
        if (connection.performEditorAction(EditorInfo.IME_ACTION_SEND)) return
        val now = System.currentTimeMillis()
        connection.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0))
        connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER, 0))
    }

    private fun sendArrow(keyCode: Int) {
        val c = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        c.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        c.sendKeyEvent(KeyEvent(System.currentTimeMillis(), now, KeyEvent.ACTION_UP, keyCode, 0))
    }

    private fun copySelection() {
        val selected = currentInputConnection?.getSelectedText(0)?.toString()
        if (selected.isNullOrEmpty()) { Toast.makeText(this, "Select text first", Toast.LENGTH_SHORT).show(); return }
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Copied text", selected))
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(this)?.toString()
        if (!text.isNullOrEmpty()) typeText(text)
    }

    private fun typeText(text: String) {
        currentInputConnection?.let { undoStack.add(text); redoStack.clear(); it.commitText(text, 1) }
    }
    private fun undo() {
        val last = undoStack.removeLastOrNull() ?: return
        currentInputConnection?.deleteSurroundingText(last.length, 0); redoStack.add(last)
    }
    private fun redo() {
        val last = redoStack.removeLastOrNull() ?: return
        currentInputConnection?.commitText(last, 1); undoStack.add(last)
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
