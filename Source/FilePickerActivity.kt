package com.easykeyboard.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class FilePickerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pick = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*"; addCategory(Intent.CATEGORY_OPENABLE) }
        startActivityForResult(Intent.createChooser(pick, "انتخاب فایل"), 100)
    }
    @Deprecated("Deprecated by Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 100 && resultCode == RESULT_OK && data?.data != null) {
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
