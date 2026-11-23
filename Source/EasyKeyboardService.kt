package com.easykeyboard.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.ExtractedTextRequest
import android.text.InputType
import android.widget.Button
import android.widget.EditText
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
    private var selectMode = false
    private var layoutMode = "standard"
    private data class RangeDeletion(val originalStart: Int, val compactedStart: Int, val text: String)
    private data class EditAction(
        val removedText: String,
        val insertedText: String,
        val rangeDeletions: List<RangeDeletion> = emptyList()
    )
    private val undoStack = mutableListOf<EditAction>()
    private val redoStack = mutableListOf<EditAction>()
    private val defaultSuggestionWords = listOf(
        "آیا", "از", "اگر", "اما", "آن", "آنها", "این", "اینجا" ,
        "اینجاها", "برای", "با", "بدون", "به", "بر", "بلکه", "بین" ,
        "تا", "توی", "چون", "چرا", "چطور", "چند", "چه", "چیز" ,
        "چیزی", "چگونه", "در", "درباره", "دوباره", "را", "زیرا", "زیر" ,
        "سپس", "سمت", "شاید", "فقط", "که", "کدام", "کم", "کمتر" ,
        "کنار", "کی", "گویا", "مگر", "مثل", "من", "میان", "نه" ,
        "نیز", "و", "یا", "یک", "هم", "همان", "همه", "هر" ,
        "بالا", "بالایی", "پایین", "پایینی", "پشت", "جلو", "چپ", "راست" ,
        "وسط", "کناری", "ردیف", "ستون", "صفحه", "صفحهٔ", "اصلی", "داخلی" ,
        "گوشه", "گوشه‌ها", "فاصله", "فضا", "خالی", "جای", "مکان", "اندازه" ,
        "عرض", "ارتفاع", "طول", "شکل", "مربع", "دایره", "پنجره", "کادر" ,
        "بخش", "قسمت", "نوار", "فهرست", "لیست", "خانه", "چیدمان", "ترتیب" ,
        "چندگانه", "یکسان", "هم‌اندازه", "ثابت", "متحرک", "کشویی", "پهن", "دکمه" ,
        "کلید", "کلیدها", "فشردن", "زدن", "لمس", "گرفتن", "نگه‌داشتن", "رهاکردن" ,
        "انتخاب", "انتخابی", "گزینه", "گزینه‌ها", "فعال", "غیرفعال", "روشن", "خاموش" ,
        "باز", "بسته", "بازکردن", "بستن", "افزودن", "اضافه", "حذف", "پاک‌کردن" ,
        "ذخیره", "ذخیره‌کردن", "ارسال", "فرستادن", "بازگشت", "برگشت", "لغو", "تأیید" ,
        "تغییر", "جابجایی", "حرکت", "کشیدن", "رها", "فشار", "ورودی", "خروجی" ,
        "نوشتن", "خواندن", "واردکردن", "نمایش", "پنهان", "پنهان‌کردن", "متن", "کلمه" ,
        "واژه", "عبارت", "جمله", "حرف", "حروف", "الفبا", "زبان", "فارسی" ,
        "انگلیسی", "عدد", "اعداد", "شماره", "نشانه", "علامت", "علائم", "نماد" ,
        "نمادها", "اعراب", "املا", "نوشتار", "نوشته", "تایپ", "صفحه‌کلید", "پیشنهاد" ,
        "پیشنهادی", "پیشنهادها", "ویرایش", "ویرایشگر", "جست‌وجو", "پیدا", "یافتن", "جایگزین" ,
        "تکرار", "یکتا", "تکراری", "فاصله‌گذاری", "نقطه", "ویرگول", "پرسش", "پرسشی" ,
        "پاسخ", "رنگ", "رنگی", "سفید", "سیاه", "زرد", "آبی", "قرمز" ,
        "قهوه‌ای", "تیره", "کم‌رنگ", "پررنگ", "بزرگ", "بزرگ‌تر", "کوچک", "کوچک‌تر" ,
        "ضخیم", "نازک", "شفاف", "پس‌زمینه", "حاشیه", "قاب", "گوشه‌گرد", "فونت" ,
        "قلم", "اندازهٔ‌قلم", "متناسب", "پروژه", "برنامه", "نرم‌افزار", "فایل", "پوشه" ,
        "مسیر", "مخزن", "نسخه", "نسخه‌ها", "کد", "کدنویسی", "ساخت", "ساختن" ,
        "اصلاح", "درست‌کردن", "رفع", "خطا", "اشکال", "مشکل", "آزمایش", "بررسی" ,
        "اجرا", "نصب", "بارگیری", "دریافت", "دانلود", "فشرده", "نام", "عنوان" ,
        "توضیح", "محتوا", "کامل", "ناقص", "تغییرات", "افزوده", "حذف‌شده", "نگهداری" ,
        "انتقال", "ذخیره‌سازی", "فلش", "جهت", "لغزش", "پیمایش", "اسکرول", "چرخش" ,
        "توقف", "شروع", "پایان", "مرحله", "گام", "همزمان", "مستقل", "خودکار" ,
        "دستی", "پیوسته", "موقت", "قابل", "تصویر", "عکس", "عکس‌ها", "فیلم" ,
        "ویدیو", "ویدئویی", "صدا", "صوت", "بلندی", "روشنایی", "نور", "تاریکی" ,
        "برش", "فریم", "نما", "نمایشگر", "پخش", "پخش‌کردن", "ضبط", "رسانه" ,
        "زیرنویس", "پیش‌نمایش", "زمان", "مدت", "اسلاید", "داستان‌نما", "دستگاه", "گوشی" ,
        "اندروید", "رایانه", "حافظه", "ذخیره‌گاه", "اتصال", "قطع", "وصل", "شبکه" ,
        "اینترنت", "نشانی", "سرور", "کشور", "عمومی", "خصوصی", "پاسخ‌گویی", "سرعت" ,
        "کارکرد", "عملکرد", "دسترسی", "اجازه", "امنیت", "درایو", "فضای‌ذخیره‌سازی", "باشد" ,
        "باشند", "بشود", "بشنود", "بده", "بگذار", "بیار", "بیاور", "بماند" ,
        "بمانند", "بکند", "کن", "کند", "کنند", "کرد", "کردن", "کرده" ,
        "می‌شود", "می‌شوند", "می‌آید", "می‌آیند", "می‌خواهد", "می‌خواهم", "می‌توان", "می‌توانی" ,
        "می‌توانند", "می‌دهم", "می‌گیرد", "می‌ماند", "می‌مانند", "می‌نویسد", "می‌سازد", "می‌زند" ,
        "می‌چرخد", "می‌کشد", "می‌افتد", "نکن", "نکند", "نشود", "نباشد", "نباشند" ,
        "ندارد", "نداشته", "نیست", "هست", "هستند", "است", "شد", "شده" ,
        "شود", "شوند", "دارم", "دارد", "دارند", "داشت", "داد", "داده" ,
        "گذاشت", "گذاشته", "گرفته", "ساخته", "بزرگ‌ترکن", "کوچک‌کن", "اضافه‌کن", "حذف‌کن" ,
        "ثابت‌کن", "فعال‌کن", "غیرفعال‌کن"
    )

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
        layoutMode = appearance.getString("layout_mode", layoutMode) ?: "standard"
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
        val suggestionPool = (defaultSuggestionWords + stored).distinct()
        val saved = if (currentToken.isBlank()) suggestionPool
            else suggestionPool.filter { it.startsWith(currentToken) && it != currentToken }
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

        // One active line-range field. Enter start-end and press Done to
        // replace that range with the current clipboard text.
        val rangeField = EditText(this).apply {
            hint = "Lines: 5-12"
            textSize = 11f
            gravity = Gravity.CENTER
            textDirection = View.TEXT_DIRECTION_LTR
            setSingleLine(true)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isEnabled = true
            setSelectAllOnFocus(true)
            showSoftInputOnFocus = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(dp(3), 0, dp(3), 0)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(5).toFloat()
                setStroke(dp(1), Color.GRAY)
            }
            setOnEditorActionListener { _, actionId, event ->
                val done = actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.let { it.keyCode == KeyEvent.KEYCODE_ENTER && it.action == KeyEvent.ACTION_DOWN } == true)
                if (done) {
                    replaceLineRangeFromClipboard(text.toString())
                    true
                } else false
            }
        }

        // Additional action row above the main toolbar
        val actionRowScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
        }
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        actionRow.addView(rangeField, LinearLayout.LayoutParams(dp(104), dp(48)).apply {
            setMargins(dp(2), 0, dp(2), 0)
        })
        listOf("Select", "Setup", "Save Suggestion").forEach { label ->
            val action = when (label) {
                "Save Suggestion" -> ({ saveSuggestion() })
                "Select" -> ({ toggleSelectMode() })
                else -> ({ showSetup() })
            }
            val buttonLabel = if (label == "Save Suggestion") "Save\nSuggestion" else label
            val button = key(buttonLabel, true, doubleLabel = label != "Save Suggestion") { action() }
            val screenWidthDp = resources.displayMetrics.widthPixels / resources.displayMetrics.density
            // Match the width of a regular letter key in the third alphabet row.
            val width = when (label) {
                "Save Suggestion" -> dp((screenWidthDp / if (persian) 13.15f else 11.15f).toInt())
                "Setup" -> dp(62)
                else -> dp(76)
            }
            button.isEnabled = true
            button.isClickable = true
            button.isFocusable = true
            if (label == "Setup") {
                button.isSingleLine = true
                button.textSize = 12f
            }
            if (label == "Save Suggestion") {
                button.isSingleLine = false
                button.textSize = 9f
                button.setLineSpacing(0f, 0.9f)
                button.setPadding(dp(2), 0, dp(2), 0)
            }
            if (label == "Select" && selectMode) button.setBackgroundColor(Color.rgb(255, 220, 35))
            actionRow.addView(button, LinearLayout.LayoutParams(width, dp(48)))
        }
        actionRowScroll.addView(actionRow)
        outer.addView(actionRowScroll, LinearLayout.LayoutParams(-1, dp(48)))

        // Second control row is fixed-width; it no longer scrolls horizontally.
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 11f
        }
        val toolbarKeys = listOf("«", "»", "−", "_", "Undo", "Redo", "Past", "Copy", "Emoji", "File", "Answer")
        toolbarKeys.forEach { label ->
            val b = key(label, true, doubleLabel = false) {
                when (label) {
                    "Undo" -> undo()
                    "Redo" -> redo()
                    "Past" -> pasteFromClipboard()
                    "Copy" -> copySelection()
                    "Save Suggestion" -> saveSuggestion()
                    "Export List" -> exportSuggestions()
                    "Emoji" -> showEmojiPicker()
                    "File" -> {
                        val intent = android.content.Intent(this, FilePickerActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
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
            b.textSize = if (label.length > 5) 9f else 11f
            toolbar.addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
        outer.addView(toolbar, LinearLayout.LayoutParams(-1, dp(48)))

        if (layoutMode == "computer") {
            val pcRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            listOf(
                "Tab" to KeyEvent.KEYCODE_TAB,
                "Esc" to KeyEvent.KEYCODE_ESCAPE,
                "Ctrl" to KeyEvent.KEYCODE_CTRL_LEFT,
                "Alt" to KeyEvent.KEYCODE_ALT_LEFT,
                "Home" to KeyEvent.KEYCODE_MOVE_HOME,
                "End" to KeyEvent.KEYCODE_MOVE_END
            ).forEach { (label, code) ->
                pcRow.addView(key(label, true) { sendKeyCode(code) },
                    LinearLayout.LayoutParams(0, dp(40), 1f))
            }
            outer.addView(pcRow, LinearLayout.LayoutParams(-1, dp(40)))
            numberRowVisible = true
        } else if (layoutMode == "mouse") {
            val mouseRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            listOf(
                "↑" to KeyEvent.KEYCODE_DPAD_UP,
                "←" to KeyEvent.KEYCODE_DPAD_LEFT,
                "کلیک" to KeyEvent.KEYCODE_DPAD_CENTER,
                "راست" to KeyEvent.KEYCODE_MENU,
                "→" to KeyEvent.KEYCODE_DPAD_RIGHT,
                "↓" to KeyEvent.KEYCODE_DPAD_DOWN
            ).forEach { (label, code) ->
                val control = key(label, true) { sendKeyCode(code) }
                if (code in listOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_DOWN)) {
                    enableRepeat(control) { sendKeyCode(code) }
                }
                mouseRow.addView(control, LinearLayout.LayoutParams(0, dp(40), 1f))
            }
            outer.addView(mouseRow, LinearLayout.LayoutParams(-1, dp(40)))
        }

        if (numberRowVisible) {
            val numberRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val digits = if (persian) listOf("۱","۲","۳","۴","۵","۶","۷","۸","۹","۰")
                         else listOf("1","2","3","4","5","6","7","8","9","0")
            val cornerSymbols = listOf("#", "@", ":", "+", "(", ")", "*", "✓", "~", "•")
            val symbolChoices = listOf(
                listOf("#", "{", "}"), listOf("@", "[", "]"),
                listOf(":", "÷", "×"), listOf("+", "|", "%"),
                listOf("(", ")"), listOf("*", "="),
                listOf("✓", "~"), listOf("~", "•"),
                listOf("•", "="), listOf("=", "#")
            )
            digits.forEachIndexed { index, digit ->
                val keyView = keyWithCornerSymbol(
                    digit,
                    cornerSymbols[index],
                    longAction = { anchor -> showChoices(anchor, symbolChoices[index]) }
                ) { typeText(digit) }
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
        // Holding Shift opens the complete former Symbols collection here.
        val alternateSymbols = listOf(
            "!", "@", "#", "$", "%", "^", "&", "*", "(", ")",
            "-", "_", "+", "=", "[", "]", "{", "}", ";", "'",
            ":", "\\\"", "/", "?", "\\\\", "|", "<", ">", "×", "✓",
            "©", "®", "™", "§", "¶", "•", "…", "‰", "°", "℃",
            "℉", "№", "₽", "$", "€", "£", "¥", "¢", "₹", "₩",
            "÷", "≠", "≈", "≡", "≤", "≥", "±", "√", "∞", "∑",
            "∏", "∫", "π", "←", "→", "↑", "↓", "↔", "↕", "○",
            "●", "□", "■"
        )
        val alternateRows = alternateSymbols.chunked(10)
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
                            shift.isPressed = true
                            longPressed = false
                            val task = Runnable { longPressed = true }
                            holdTask = task
                            handler.postDelayed(task, 500)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            shift.isPressed = false
                            holdTask?.let { handler.removeCallbacks(it) }
                            if (longPressed) {
                                // Holding Shift opens the alternate-symbol keyboard.
                                symbolLayout = true
                                caps = false
                            } else if (symbolLayout) {
                                // A short tap on ABC returns to the normal letters.
                                symbolLayout = false
                                caps = false
                            } else {
                                // A short tap enables one-shot uppercase.
                                caps = true
                            }
                            setInputView(buildKeyboard())
                            true
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            shift.isPressed = false
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
                            else if (!symbolLayout
                                      // Uppercase applies to exactly one typed letter.
                        caps = false
                        setInputView(buildKeyboard())
                    }
                }
                val variants = if (!symbolLayout && persian) when (ch) {
                    "ا" -> listOf("آ", "أ", "إ")
                    "و" -> listOf("ؤ")
                    "ی" -> listOf("ئ", "ي", "ى")
                    else -> emptyList()
                } else emptyList()
                if (variants.isNotEmpty()) {
                    letterKey.isLongClickable = true
                    letterKey.setOnLongClickListener {
                        showChoices(letterKey, variants)
                        true
                    }
                }
                row.addView(letterKey, LinearLayout.LayoutParams(0, -1, 1f))
                if (!symbolLayout && persian && index == 0 && ch == "ج") {
                    val diacritics = key("اعراب", true) {
                        showGrid("اعراب", listOf("َ","ِ","ُ","ً","ٍ","ٌ","ْ","ّ","ٰ","ٓ","ٔ","ٕ"), 6)
                    }
                    diacritics.textSize = 9f
                    row.addView(diacritics, LinearLayout.LayoutParams(0, -1, 1.15f))
                }
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
            outer.addView(row, LinearLayout.LayoutParams(-1, dp(61)))
        }

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            weightSum = 12.58f
        }
        // Weighted widths fill the entire bottom row with no blank gaps or horizontal scrolling.
        fun addBottom(label: String, action: () -> Unit, corner: String? = null) {
            val buttonView = if (corner == null) key(label, true, action)
                             else keyWithCornerSymbol(label, corner, action = action)
            bottom.addView(buttonView, LinearLayout.LayoutParams(0, -1, 1f))
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
        // Keep the four navigation arrows directly between language and Space.
        listOf(
            Triple("←", KeyEvent.KEYCODE_DPAD_LEFT, "|"),
            Triple("→", KeyEvent.KEYCODE_DPAD_RIGHT, "~"),
            Triple("↑", KeyEvent.KEYCODE_DPAD_UP, "#"),
            Triple("↓", KeyEvent.KEYCODE_DPAD_DOWN, null)
        ).forEach { (label, code, corner) ->
            val arrow = if (corner == null) key(label, true) { sendArrow(code, selectMode) }
                        else keyWithCornerSymbol(label, corner) { sendArrow(code, selectMode) }
            enableRepeat(arrow) { sendArrow(code, selectMode) }
            bottom.addView(arrow, LinearLayout.LayoutParams(0, -1, 1f))
        }
        val space = key("Space", true) { typeText(" ") }
        bottom.addView(space, LinearLayout.LayoutParams(0, -1, 2.4f))
        addBottom(".", { typeText(".") }, ";")
        val circle = key("○", true) { typeText("○") }
        bottom.addView(circle, LinearLayout.LayoutParams(0, -1, 1.18f))
        addBottom("؟", { typeText("؟") }, "✓")
        addBottom("،", { typeText("،") })

        outer.addView(bottom, LinearLayout.LayoutParams(-1, (dp(58) * keyboardScale).toInt()))
        return outer
    }

    private fun key(label: String, dark: Boolean, action: () -> Unit): Button =
        key(label, dark, true, action)

    private fun key(
        label: String,
        dark: Boolean,
        doubleLabel: Boolean,
        action: () -> Unit
    ): Button = Button(this).apply {
            text = label
             isAllCaps = false
            val baseTextSize = when {
                label.length == 1 && (label[0].isLetterOrDigit() ||
                    label[0] in "!@#$%^&*()-_=+[]{};:'\"/\\?|<>×✓©®™§¶•…‰°℃℉№₽€£¥¢₹₩÷≠≈≡≤≥±√∞∑∏∫π○●□■،؟؛٫٬") -> 45f
                label.length > 9 -> 10f
                label.length > 5 -> 12f
                else -> 15f
            }
            textSize = when (label) {
                "← Backspace", "Backspace" -> 9f
                "123" -> 13f
                else -> if (doubleLabel && label.length > 1) baseTextSize * 2f else baseTextSize
            }
            if (label == "← Backspace" || label == "Backspace" || label == "123") isSingleLine = true
            setTextColor(buttonTextColor)
            includeFontPadding = false
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            minWidth = 0
            minHeight = 0
            setPadding(dp(2), 0, dp(2), 0)
            isSoundEffectsEnabled = false
            val keyFill = when {
                label == "Enter" -> Color.rgb(190, 220, 248)
                label == "← Backspace" || label == "Backspace" -> Color.rgb(248, 190, 190)
                label.length == 1 && label[0].isDigit() -> Color.rgb(220, 190, 150)
                else -> Color.rgb(250, 247, 235)
            }
            fun keyShape(fill: Int) = GradientDrawable().apply {
                setColor(fill)
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), Color.rgb(160, 170, 185))
            }
            background = StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_pressed), keyShape(Color.rgb(255, 225, 35)))
                addState(intArrayOf(), keyShape(keyFill))
            }
            isClickable = true
            isFocusable = true
            setOnClickListener { action() }
        }

    private fun showChoices(anchor: View, choices: List<String>) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            setBackgroundColor(Color.WHITE)
        }
        val popup = PopupWindow(content, -2, -2, true).apply {
            isOutsideTouchable = true
            elevation = dp(8).toFloat()
        }
        choices.forEach { value ->
            content.addView(key(value, false) {
                typeText(value)
                popup.dismiss()
            }, LinearLayout.LayoutParams(dp(48), dp(50)))
        }
        popup.showAsDropDown(anchor, 0, -dp(56))
    }

    private fun keyWithCornerSymbol(
        label: String,
        corner: String,
        longAction: ((View) -> Unit)? = null,
        action: () -> Unit
    ): View {
        val box = FrameLayout(this)
        val main = key(label, true, action)
        if (longAction != null) main.setOnLongClickListener { longAction(main); true }
        box.addView(main, FrameLayout.LayoutParams(-1, -1))
        val mark = TextView(this).apply {
            text = corner
            textSize = 8f
            setTextColor(buttonTextColor)
            gravity = Gravity.TOP or Gravity.LEFT
            setPadding(dp(2), dp(1), 0, 0)
            isClickable = false
            isFocusable = false
        }
        box.addView(mark, FrameLayout.LayoutParams(-1, -1))
        return box
    }

    private fun enableRepeat(view: View, action: () -> Unit) {
        var task: Runnable? = null
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view.isPressed = true
                    task?.let { handler.removeCallbacks(it) }
                    action()
                    val repeat = object : Runnable {
                        override fun run() {
                            action()
                            handler.postDelayed(this, 75)
                        }
                    }
                    task = repeat
                    handler.postDelayed(repeat, 380)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.isPressed = false
                    task?.let { handler.removeCallbacks(it) }
                    task = null
                    true
                }
                else -> true
            }
        }
    }
    
    private fun removeSuggestion(word: String) {
        val prefs = getSharedPreferences("suggestions", Context.MODE_PRIVATE)
        val items = prefs.getStringSet("items", emptySet())?.toMutableSet() ?: mutableSetOf()
        items.remove(word)
        prefs.edit().putStringSet("items", items).apply()
        setInputView(buildKeyboard())
    }

    private fun toggleSelectMode() {
        selectMode = !selectMode
        setInputView(buildKeyboard())
    }

    private fun showSetup() {
        val options = arrayOf("Standard", "Computer", "Mouse")
        val current = when (layoutMode) { "computer" -> 1; "mouse" -> 2; else -> 0 }
        AlertDialog.Builder(this)
            .setTitle("Keyboard layout")
            .setSingleChoiceItems(options, current) { dialog, which ->
                layoutMode = when (which) { 1 -> "computer"; 2 -> "mouse"; else -> "standard" }
                getSharedPreferences("appearance", Context.MODE_PRIVATE).edit()
                    .putString("layout_mode", layoutMode).apply()
                dialog.dismiss()
                setInputView(buildKeyboard())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportSuggestions() {
        val items = getSharedPreferences("suggestions", Context.MODE_PRIVATE)
            .getStringSet("items", emptySet())?.toList().orEmpty().sorted()
        val file = java.io.File(filesDir, "EasyKeys_Suggestions.txt")
        file.writeText(items.joinToString("\n"), Charsets.UTF_8)
        Toast.makeText(this, "Saved: ${file.name}", Toast.LENGTH_LONG).show()
    }

    private fun sendKeyCode(code: Int) {
        val connection = currentInputConnection ?: return
        val now = System.currentTimeMillis()
        connection.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0))
        connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), now, KeyEvent.ACTION_UP, code, 0))
    }

    private fun sendArrow(keyCode: Int, selecting: Boolean = false) {
        val connection = currentInputConnection ?: return
        val meta = if (selecting) KeyEvent.META_SHIFT_ON else 0
        val now = System.currentTimeMillis()
        connection.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, meta))
        connection.sendKeyEvent(KeyEvent(System.currentTimeMillis(), now, KeyEvent.ACTION_UP, keyCode, meta))
    }

    private fun saveTextRanges(rangeTexts: List<String>, cut: Boolean) {
        val connection = currentInputConnection
        val request = ExtractedTextRequest().apply {
            token = 0
            hintMaxChars = 100000
            hintMaxLines = 10000
        }
        val extracted = connection?.getExtractedText(request, 0)
        val fullText = extracted?.text?.toString()
        if (connection == null || extracted == null || fullText == null) {
            Toast.makeText(this, "Cannot read editor text", Toast.LENGTH_SHORT).show()
            return
        }
        val lines = fullText.split("\n")
        val starts = IntArray(lines.size)
        var cursor = 0
        lines.forEachIndexed { i, line -> starts[i] = cursor; cursor += line.length + 1 }
        val ranges = mutableListOf<Triple<Int, Int, String>>()
        rangeTexts.forEach { raw ->
            val normalized = raw
                .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3')
                .replace('۴', '4').replace('۵', '5').replace('۶', '6').replace('۷', '7')
                .replace('۸', '8').replace('۹', '9')
                .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
                .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
                .replace('٨', '8').replace('٩', '9')
            val match = Regex("^\\s*(\\d+)\\s*[-–—]\\s*(\\d+)\\s*$").matchEntire(normalized)
            if (match != null) {
                val first = match.groupValues[1].toIntOrNull() ?: return@forEach
                val last = match.groupValues[2].toIntOrNull() ?: return@forEach
                if (first >= 1 && last >= first && last <= lines.size) {
                    val begin = starts[first - 1]
                    val finish = if (last < lines.size) starts[last] - 1 else fullText.length
                    ranges.add(Triple(begin, finish, lines.subList(first - 1, last).joinToString("\n")))
                }
            }
        }
        if (ranges.isEmpty()) {
            Toast.makeText(this, "Enter valid line ranges, e.g. 5-12", Toast.LENGTH_SHORT).show()
            return
        }
        val output = ranges.joinToString("\n\n--------------------\n\n") { it.third }
        val file = java.io.File(filesDir, "EasyKeys_RangeCopies.txt")
        file.writeText(output, Charsets.UTF_8)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("EasyKeys ranges", output))
        if (cut) {
            var removedBefore = 0
            val undoRanges = ranges.sortedBy { it.first }.map { range ->
                val compacted = extracted.startOffset + range.first - removedBefore
                removedBefore += range.second - range.first
                RangeDeletion(
                    originalStart = extracted.startOffset + range.first,
                    compactedStart = compacted,
                    text = range.third
                )
            }
            ranges.sortedByDescending { it.first }.forEach { range ->
                val start = extracted.startOffset + range.first
                val end = extracted.startOffset + range.second
                connection.setSelection(start, end)
                connection.commitText("", 1)
            }
            undoStack.add(EditAction("", "", undoRanges))
            redoStack.clear()
        }
        Toast.makeText(this, "Saved ${ranges.size} range(s): ${file.name}", Toast.LENGTH_LONG).show()
    }

    private fun replaceLineRangeFromClipboard(rawRange: String) {
        val connection = currentInputConnection
        val extracted = connection?.getExtractedText(ExtractedTextRequest().apply {
            token = 0
            hintMaxChars = 100000
            hintMaxLines = 10000
        }, 0)
        val fullText = extracted?.text?.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val replacement = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)?.coerceToText(this)?.toString()
        if (connection == null || extracted == null || fullText == null) {
            Toast.makeText(this, "Cannot read editor text", Toast.LENGTH_SHORT).show()
            return
        }
        if (replacement.isNullOrEmpty()) {
            Toast.makeText(this, "Copy the replacement text first", Toast.LENGTH_SHORT).show()
            return
        }
        val normalized = rawRange
            .replace('۰', '0').replace('۱', '1').replace('۲', '2').replace('۳', '3')
            .replace('۴', '4').replace('۵', '5').replace('۶', '6').replace('۷', '7')
            .replace('۸', '8').replace('۹', '9')
            .replace('٠', '0').replace('١', '1').replace('٢', '2').replace('٣', '3')
            .replace('٤', '4').replace('٥', '5').replace('٦', '6').replace('٧', '7')
            .replace('٨', '8').replace('٩', '9')
        val match = Regex("^\\s*(\\d+)\\s*[-–—]\\s*(\\d+)\\s*$").matchEntire(normalized)
        val first = match?.groupValues?.get(1)?.toIntOrNull()
        val last = match?.groupValues?.get(2)?.toIntOrNull()
        val lines = fullText.split("\n")
        if (first == null || last == null || first < 1 || last < first || last > lines.size) {
            Toast.makeText(this, "Enter a valid range, e.g. 5-12", Toast.LENGTH_SHORT).show()
            return
        }
        val starts = IntArray(lines.size)
        var cursor = 0
        lines.forEachIndexed { index, line ->
            starts[index] = cursor
            cursor += line.length + 1
        }
        val start = extracted.startOffset + starts[first - 1]
        val endInText = if (last < lines.size) starts[last] else fullText.length
        val end = extracted.startOffset + endInText
        val textToInsert = if (last < lines.size) replacement.trimEnd('\n') + "\n"
            else replacement
        connection.beginBatchEdit()
        try {
            connection.setSelection(start, end)
            connection.commitText(textToInsert, 1)
            undoStack.add(EditAction(fullText.substring(starts[first - 1], endInText), textToInsert))
            redoStack.clear()
        } finally {
            connection.endBatchEdit()
        }
        Toast.makeText(this, "Lines $first-$last replaced from clipboard", Toast.LENGTH_SHORT).show()
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
            "Folders & Files" to listOf("📁","📂","🗂️","🗃️","🗄️","📄","📃","📑","🧾","📜","📋","📇","🗒️","🗓️","📅","📆","🗞️","📰","📓","📔","📒","📕","📗","📘","📙","📚","📖","🔖","📌","📍","✂️","🖇️","📎","🖊️","🖋️","✒️","🖌️","🖍️","📝"),
            "Asia Flags" to listOf("🇮🇷","🇨🇳","🇮🇳","🇯🇵","🇰🇷","🇸🇦","🇹🇷","🇵🇰","🇮🇩","🇹🇭","🇻🇳","🇲🇾","🇸🇬","🇦🇪","🇶🇦","🇮🇶","🇦🇫","🇧🇩","🇱🇰","🇳🇵"),
            "Europe Flags" to listOf("🇬🇧","🇫🇷","🇩🇪","🇮🇹","🇪🇸","🇵🇹","🇳🇱","🇧🇪","🇨🇭","🇦🇹","🇸🇪","🇳🇴","🇫🇮","🇩🇰","🇬🇷","🇵🇱","🇺🇦","🇷🇺","🇮🇪","🇨🇿"),
            "Africa Flags" to listOf("🇪🇬","🇿🇦","🇳🇬","🇰🇪","🇲🇦","🇩🇿","🇹🇳","🇬🇭","🇪🇹","🇹🇿","🇺🇬","🇸🇳","🇨🇮","🇨🇲","🇦🇴","🇸🇩","🇱🇾","🇿🇼","🇧🇼","🇷🇼"),
            "N. America Flags" to listOf("🇺🇸","🇨🇦","🇲🇽","🇨🇺","🇯🇲","🇵🇦","🇨🇷","🇬🇹","🇭🇳","🇩🇴","🇭🇹","🇧🇸","🇧🇧","🇹🇹","🇧🇿"),
            "S. America Flags" to listOf("🇧🇷","🇦🇷","🇨🇱","🇨🇴","🇵🇪","🇻🇪","🇪🇨","🇺🇾","🇵🇾","🇧🇴","🇬🇾","🇸🇷"),
            "Oceania Flags" to listOf("🇦🇺","🇳🇿","🇫🇯","🇵🇬","🇼🇸","🇹🇴","🇻🇺","🇸🇧","🇰🇮","🇫🇲","🇵🇼","🇲🇭")
        )
        // Keep each emoji category scrollable with at least 70 distinct entries.
        val supplementalEmoji = listOf(
            "🫠","🫢","🫣","🫡","🫥","🫤","🥹","🫶","🫰","🤌",
            "🤏","🫳","🫴","🫱","🫲","🫸","🫷","🫵","🫦","🧏",
            "🧑","👨","👩","🧒","👶","👵","👴","🧔","👮","👷",
            "🧑‍⚕️","🧑‍🍳","🧑‍🎓","🧑‍🏫","🧑‍💻","🧑‍🔧","🧑‍🚀","🧑‍🎨","🧑‍🚒","🧑‍✈️",
            "🐈‍⬛","🦬","🦣","🦫","🦥","🦦","🦨","🦡","🦤","🪿",
            "🪼","🪸","🪷","🪻","🪴","🪹","🪺","🪵","🪨","🪬",
            "🫧","🪩","🪅","🪆","🪇","🪈","🪉","🪏","🪙","🪪"
        )
        groups.keys.toList().forEach { name ->
            groups[name] = (groups[name].orEmpty() + supplementalEmoji).distinct().take(70)
        }
        groups["Colored Shapes"] = listOf(
            "🔴","🟠","🟡","🟢","🔵","🟣","🟤","⚫","⚪","🟥",
            "🟧","🟨","🟩","🟦","🟪","🟫","⬛","⬜","🔶","🔷",
            "🔸","🔹","🔺","🔻","💠","🔘","🔳","🔲","◼️","◻️",
            "◾","◽","▪️","▫️","❤️","🧡","💛","💚","💙","💜",
            "🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖",
            "💘","💝","💟","♥️","♡","✨","⭐","🌟","☀️","🌙",
            "☁️","⚡","🔥","💧","🌈","❄️","🌸","🌹","🌻","🍀"
        ).distinct().take(70)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(6), dp(6), dp(6), dp(6)) }
        val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val tabsScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabs)
        }
        val popup = PopupWindow(root, -1, dp(300), true).apply { isOutsideTouchable = true; elevation = dp(8).toFloat() }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(content) }
        root.addView(tabsScroll, LinearLayout.LayoutParams(-1, dp(42)))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        fun render(items: List<String>) {
            content.removeAllViews()
            items.chunked(8).forEach { group ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                group.forEach { emoji -> row.addView(key(emoji, false, false) { typeText(emoji); popup.dismiss() }, LinearLayout.LayoutParams(0, dp(42), 1f)) }
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

    private fun deleteOne() {
        val connection = currentInputConnection ?: return
        val selected = connection.getSelectedText(0)?.toString().orEmpty()
        val removed = if (selected.isNotEmpty()) selected else {
            val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
            if (before.isEmpty()) "" else {
                val last = before.codePointBefore(before.length)
                String(Character.toChars(last))
            }
        }
        if (removed.isEmpty()) return
        if (selected.isNotEmpty()) connection.commitText("", 1)
        else connection.deleteSurroundingText(removed.length, 0)
        undoStack.add(EditAction(removed, ""))
        redoStack.clear()
    }

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
        val connection = currentInputConnection ?: return
        val removed = connection.getSelectedText(0)?.toString().orEmpty()
        connection.commitText(text, 1)
        if (removed != text) {
            undoStack.add(EditAction(removed, text))
            redoStack.clear()
        }
    }

    private fun undo() {
        val connection = currentInputConnection ?: return
        val action = undoStack.removeLastOrNull() ?: return
        if (action.rangeDeletions.isNotEmpty()) {
            action.rangeDeletions.sortedByDescending { it.compactedStart }.forEach { range ->
                connection.setSelection(range.compactedStart, range.compactedStart)
                connection.commitText(range.text, 1)
            }
        } else {
            if (action.insertedText.isNotEmpty()) {
                connection.dele
