package com.easykeyboard.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class FilePickerActivity : Activity() {
    private val exportRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("export_suggestions", false)) {
            val create = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
                putExtra(Intent.EXTRA_TITLE, "Easy_Keys_Suggestions.txt")
            }
            startActivityForResult(create, exportRequestCode)
        } else {
            val pick = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(Intent.createChooser(pick, "انتخاب فایل"), 100)
        }
    }

    @Deprecated("Deprecated by Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == exportRequestCode) {
            if (resultCode == RESULT_OK && data?.data != null) {
                try {
                    val saved = getSharedPreferences("suggestions", MODE_PRIVATE)
                        .getStringSet("items", emptySet())?.toList().orEmpty()
                    val defaults = listOf("نه", "بله", "چرا", "این", "در", "که", "زیرا", "و", "بلکه", "همه", "یک", "اینجا", "موارد")
                    val hidden = getSharedPreferences("suggestions", MODE_PRIVATE)
                        .getStringSet("hidden_defaults", emptySet()) ?: emptySet()
                    val all = (defaults.filterNot { it in hidden } + saved).distinct().sorted()
                    contentResolver.openOutputStream(data.data!!)?.use { output ->
                        output.write(all.joinToString("\n").toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Cannot open selected file")
                    Toast.makeText(this, "فهرست پیشنهادها ذخیره شد", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "خطا در ذخیره فهرست: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            finish()
            return
        }
        if (requestCode == 100 && resultCode == RESULT_OK && data?.data != null) {
            val share = Intent(Intent.ACTION_SEND).apply {
                type = contentResolver.getType(data.data!!) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, data.data)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "ارسال فایل"))
        }
        finish()
    }
}
