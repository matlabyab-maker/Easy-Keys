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
import android.inputmethodservice.InputMethodService

class EasyKeyboardService : InputMethodService() {
    private var persian = true
    private var caps = false
    private var numberRowVisible = false
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private val handler = Handler(Looper.getMainLooper())
    private val repeatTasks = mutableMapOf<View, Runnable>()

    override fun onCreateInputView(): View = buildKeyboard()

    private fun buildKeyboard(): View {
        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(2), dp(2), dp(2), dp(2))
            setBackgroundColor(Color.rgb(190, 198, 208))
        }

        // Personal suggestion strip
        val suggestions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val saved = getSharedPreferences("suggestions", Context.MODE_PRIVATE)
            .getStringSet("items", emptySet())?.toList().orEmpty()
        val suggestionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(suggestions)
        }
        if (saved.isEmpty()) {
            suggestions.addView(TextView(this).apply {
                text = "Suggestions"
                textSize = 12f
                setTextColor(Color.DKGRAY)
                setPadding(dp(8), dp(4), dp(8), dp(4))
            })
        } else saved.forEach { word ->
            suggestions.addView(key(word, false) { typeText(word) },
                LinearLayout.LayoutParams(-2, dp(34)))
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
                else -> ({ /* Action to be configured */ })
            }
            val button = key(label, true, action)
            val width = if (label == "Save Suggestion") dp(61) else dp(76)
            actionRow.addView(button, LinearLayout.LayoutParams(width, dp(48)))
        }
        actionRowScroll.addView(actionRow)
        outer.addView(actionRowScroll, LinearLayout.LayoutParams(-1, dp(48)))

        val toolbarScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val toolbarKeys = listOf("«", "»", "○", "−", "_", "؟", "Undo", "Redo", "Past", "Copy", "File", "Answer")
        toolbarKeys.forEach { label ->
            val b = key(label, true) {
                when (label) {
                    "Undo" -> undo()
                    "Redo" -> redo()
                    "Past" -> pasteFromClipboard()
                    "Copy" -> copySelection()
                    "Save Suggestion" -> saveSuggestion()
                    "File" -> {
                        val intent = android.content.Intent(this, FilePickerActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }
                    "Answer" -> answerCurrentEditor()
                    "○" -> typeText("○")
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
                if (label == "Answer") dp(82) else dp(if (label == "Save Suggestion") 61 else if (label.length > 8) 92 else if (label.length > 4) 64 else 48),
                -1
            ))
        }
        toolbarScroll.addView(toolbar)
        outer.addView(toolbarScroll, LinearLayout.LayoutParams(-1, dp(48)))

        if (numberRowVisible) {
            val numberRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            listOf("1","2","3","4","5","6","7","8","9","0").forEach { digit ->
                val digitButton = key(digit, false) { typeText(digit) }
                enableRepeat(digitButton) { typeText(digit) }
                numberRow.addView(digitButton, LinearLayout.LayoutParams(0, dp(44), 1f))
            }
            outer.addView(numberRow, LinearLayout.LayoutParams(-1, dp(44)))
        }

        val rows = if (persian) listOf(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج"),
            listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ"),
            listOf("ظ", "ط", "ژ", "ز", "ر", "ذ", "د", "پ", "و", "چ")
        ) else listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
            listOf("Z", "X", "C", "V", "B", "N", "M")
        )

        rows.forEachIndexed { index, chars ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            if (index == 2) row.addView(key("Shift", true) {
                caps = !caps
                setInputView(buildKeyboard())
            }, LinearLayout.LayoutParams(0, -1, 1.35f))
            chars.forEach { ch ->
                val shown = if (!persian && caps) ch.uppercase() else if (!persian) ch.lowercase() else ch
                val letterKey = key(shown, false) { typeText(shown) }
                val variants = if (persian) when (ch) {
                    "ا" -> listOf("آ", "أ", "إ")
                    "و" -> listOf("ؤ")
                    "ی" -> listOf("ئ", "ي", "ى")
                    else -> emptyList()
                } else emptyList()
                if (variants.isNotEmpty()) letterKey.setOnLongClickListener {
                    showChoices(letterKey, variants); true
                }
                row.addView(letterKey, LinearLayout.LayoutParams(0, -1, 1f))
                if (!persian && index == 2 && ch == "M") {
                    row.addView(key(",", true) { typeText(",") },
                        LinearLayout.LayoutParams(0, -1, 1f))
                }
            }
            if (index == 1) row.addView(key("Enter", true) { answerCurrentEditor() },
                LinearLayout.LayoutParams(0, -1, 1.8f))
            if (index == 2) {
                val back = key("Backspace", true) { deleteOne() }
                enableRepeat(back) { deleteOne() }
                row.addView(back, LinearLayout.LayoutParams(0, -1, 1.8f))
            }
            outer.addView(row, LinearLayout.LayoutParams(-1, dp(61)))
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }
        val screenWidthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
        val letterUnit = ((screenWidthDp - 4f) / 12.35f).coerceAtLeast(dp(18).toFloat())
        val sameAsLetter = letterUnit.toInt()
        val shiftWidth = (letterUnit * 1.35f).toInt()
        val arrowWidth = (letterUnit * 0.95f).toInt()
        fun addBottom(label: String, widthPx: Int, action: () -> Unit, longAction: (() -> Unit)? = null) {
        val b = key(label, true, action)
        if (longAction != null) b.setOnLongClickListener { longAction(); true }
        bottom.addView(b, LinearLayout.LayoutParams(widthPx, -1))
    }
    addBottom("123", sameAsLetter, {
        numberRowVisible = !numberRowVisible
        setInputView(buildKeyboard())
    }, { showSymbols() })
    addBottom(if (persian) "EN" else "FA", sameAsLetter, {
        persian = !persian
        caps = false
        setInputView(buildKeyboard())
    })
    addBottom("Space", (letterUnit * 2.2f).toInt(), { typeText(" ") })
addBottom(".", sameAsLetter, { typeText(".") })
addBottom("○", sameAsLetter, { typeText("○") })
addBottom("؟", sameAsLetter, { typeText("؟") })

    listOf(
        Triple("←", KeyEvent.KEYCODE_DPAD_LEFT, shiftWidth),
        Triple("→", KeyEvent.KEYCODE_DPAD_RIGHT, shiftWidth),
        Triple("↑", KeyEvent.KEYCODE_DPAD_UP, arrowWidth),
        Triple("↓", KeyEvent.KEYCODE_DPAD_DOWN, arrowWidth)
    ).forEach { (label, code, width) ->
        val b = key(label, true) { sendArrow(code) }
        enableRepeat(b) { sendArrow(code) }
        bottom.addView(b, LinearLayout.LayoutParams(width, -1))
    }
    outer.addView(bottom, LinearLayout.LayoutParams(-1, dp(58)))
return outer
    }

    private fun key(label: String, dark: Boolean, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = when {
                label.length >= 8 -> 9f
                label.length >= 5 -> 11f
                label.length >= 4 -> 12f
                else -> 18f
            }
            setTextColor(when {
                label == "Backspace" -> Color.rgb(105, 25, 25)
                label == "123" || label.all { it.isDigit() } -> Color.rgb(130, 75, 35)
                else -> Color.rgb(25, 35, 75)
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
                "Backspace" -> Color.rgb(244, 185, 185)
                "Enter" -> Color.rgb(190, 220, 250)
                else -> Color.rgb(247, 249, 252)
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
            isOutsideTouchable = true; elevation = dp(8).toFloat()
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
            "&","@","#","*","/","\\","|","~","^","_",
            "←","→","↑","↓","↔","↕","○","●","□","■"
        ).take(60)
        showGrid("60 Symbols", symbols, 6)
    }

    private fun showEmojiPicker() {
        val groups = linkedMapOf(
            "Faces" to listOf("😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇","🙂","🙃","😉","😍","🥰","😘","😋","😎","🤔","🤗","😭","😢","😡","🥳","😴"),
            "Animals" to listOf("🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🐔","🐧","🐦","🦆","🦉","🐴","🦄","🐝","🦋","🐢"),
            "Food" to listOf("🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🫐","🍒","🍑","🥭","🍍","🥥","🥑","🍅","🥕","🌽","🥦","🍞","🧀","🍕","🍔","🍟","🍰"),
            "Nature" to listOf("🌞","🌝","🌚","⭐","🌟","🌈","☁️","🌧️","⛈️","❄️","🔥","💧","🌊","🌸","🌹","🌻","🌳","🍀","🌵","🌴","🌍","🌙","☀️","⚡","🌪️"),
            "Activities" to listOf("⚽","🏀","🏈","⚾","🎾","🏐","🎱","🏓","🏸","🥊","🥋","🎯","🎮","🎲","🎸","🎹","🎤","🎧","🎨","🎭","🏆","🥇","🚴","🏊","⛷️"),
            "Objects" to listOf("⌚","📱","💻","⌨️","🖥️","🖨️","📷","📹","📺","📻","🔋","💡","🔦","📚","✏️","🖊️","📌","📎","🔒","🔑","🔨","⚙️","🧰","🎁","📦"),
            "Hearts" to listOf("❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","♥️","♡","💌","💋","💍","✨"),
            "Travel" to listOf("🚗","🚕","🚌","🚎","🏎️","🚓","🚑","🚒","🚲","🛵","🏍️","✈️","🚀","🚢","⛵","🚆","🚇","🚉","🏠","🏢","🏰","🗼","🗽","⛺","🗺️"),
            "Asia Flags" to listOf("🇮🇷","🇨🇳","🇮🇳","🇯🇵","🇰🇷","🇸🇦","🇹🇷","🇵🇰","🇮🇩","🇹🇭","🇻🇳","🇲🇾","🇸🇬","🇦🇪","🇶🇦","🇮🇶","🇦🇫","🇧🇩","🇱🇰","🇳🇵"),
            "Europe Flags" to listOf("🇬🇧","🇫🇷","🇩🇪","🇮🇹","🇪🇸","🇵🇹","🇳🇱","🇧🇪","🇨🇭","🇦🇹","🇸🇪","🇳🇴","🇫🇮","🇩🇰","🇬🇷","🇵🇱","🇺🇦","🇷🇺","🇮🇪","🇨🇿"),
            "Africa Flags" to listOf("🇪🇬","🇿🇦","🇳🇬","🇰🇪","🇲🇦","🇩🇿","🇹🇳","🇬🇭","🇪🇹","🇹🇿","🇺🇬","🇸🇳","🇨🇮","🇨🇲","🇦🇴","🇸🇩","🇱🇾","🇿🇼","🇧🇼","🇷🇼"),
            "N. America Flags" to listOf("🇺🇸","🇨🇦","🇲🇽","🇨🇺","🇯🇲","🇵🇦","🇨🇷","🇬🇹","🇭🇳","🇩🇴","🇭🇹","🇧🇸","🇧🇧","🇹🇹","🇧🇿"),
            "S. America Flags" to listOf("🇧🇷","🇦🇷","🇨🇱","🇨🇴","🇵🇪","🇻🇪","🇪🇨","🇺🇾","🇵🇾","🇧🇴","🇬🇾","🇸🇷"),
            "Oceania Flags" to listOf("🇦🇺","🇳🇿","🇫🇯","🇵🇬","🇼🇸","🇹🇴","🇻🇺","🇸🇧","🇰🇮","🇫🇲","🇵🇼","🇲🇭")
        )
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(6), dp(6), dp(6)) }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val popup = PopupWindow(root, -1, dp(300), true).apply { isOutsideTouchable = true; elevation = dp(8).toFloat() }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(content) }
        root.addView(tabs); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        fun render(items: List<String>) {
            content.removeAllViews()
            items.chunked(8).forEach { group ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                group.forEach { emoji -> row.addView(key(emoji, false) { typeText(emoji); popup.dismiss() }, LinearLayout.LayoutParams(0, dp(42), 1f)) }
                content.addView(row)
            }
        }
        groups.forEach { (name, items) ->
            tabs.addView(key(name, true) { render(items); popup.update() }, LinearLayout.LayoutParams(-2, dp(38)))
        }
        render(groups.values.first())
        popup.showAtLocation(window.window?.decorView ?: root, Gravity.BOTTOM, 0, dp(260))
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
                val symbolButton = key(item, false) { typeText(item); popup.dismiss() }
                enableRepeat(symbolButton) { typeText(item) }
                row.addView(symbolButton, LinearLayout.LayoutParams(0, dp(44), 1f))
            }
            content.addView(row)
        }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        popup = PopupWindow(root, -1, dp(280), true).apply { isOutsideTouchable = true; elevation = dp(8).toFloat() }
        popup.showAtLocation(window.window?.decorView ?: root, Gravity.BOTTOM, 0, dp(260))
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
