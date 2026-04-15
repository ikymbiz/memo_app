package com.example.app

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    // 色定数
    private val colorBg = Color.parseColor("#1a1a2e")
    private val colorSurface = Color.parseColor("#16213e")
    private val colorSurface2 = Color.parseColor("#0f3460")
    private val colorAccent = Color.parseColor("#e94560")
    private val colorText = Color.parseColor("#eaeaea")
    private val colorText2 = Color.parseColor("#a0a0b8")
    private val colorBorder = Color.parseColor("#2a2a4a")

    // UI要素
    private lateinit var filenameDisplay: TextView
    private lateinit var modifiedDot: View
    private lateinit var editorEditText: EditText
    private lateinit var statLines: TextView
    private lateinit var statChars: TextView
    private lateinit var statType: TextView
    private lateinit var encodingSpinner: Spinner

    // 状態管理
    private var currentUri: Uri? = null
    private var isModified = false
    private var currentFileName = "新規ファイル"
    private var suppressTextWatcher = false

    // ファイル操作用ランチャー
    private lateinit var openFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var saveFileLauncher: ActivityResultLauncher<Intent>
    private lateinit var createFileLauncher: ActivityResultLauncher<Intent>

    // dp変換ヘルパー
    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ランチャー登録
        openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri -> readFile(uri) }
            }
        }

        saveFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    writeFile(uri)
                    currentUri = uri
                    val name = getFileNameFromUri(uri)
                    currentFileName = name
                    filenameDisplay.text = name
                    statType.text = getExtension(name)
                    setModified(false)
                    Toast.makeText(this, "✅ ${name} に保存しました", Toast.LENGTH_SHORT).show()
                }
            }
        }

        createFileLauncher = saveFileLauncher

        // ルートレイアウト
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorBg)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // ===== ヘッダー =====
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(colorSurface)
            setPadding(12.dp(), 0, 12.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 56.dp()
            )
        }

        // ロゴ
        val logoText = TextView(this).apply {
            text = "note."
            setTextColor(colorText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        header.addView(logoText)

        // ファイル名表示
        filenameDisplay = TextView(this).apply {
            text = "新規ファイル"
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            maxLines = 1
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8.dp()
                marginEnd = 8.dp()
            }
        }
        header.addView(filenameDisplay)

        // 未保存ドット
        modifiedDot = View(this).apply {
            val dotBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(colorAccent)
            }
            background = dotBg
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(7.dp(), 7.dp())
        }
        header.addView(modifiedDot)

        // ヘッダー下の区切り線
        val headerDivider = View(this).apply {
            setBackgroundColor(colorBorder)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp())
        }

        root.addView(header)
        root.addView(headerDivider)

        // ===== ツールバーコンテナ =====
        val toolbarContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(colorSurface)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // ツールバー行1
        val toolbarRow1Scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp()
            )
        }
        val toolbarRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10.dp(), 0, 10.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 新規ボタン
        toolbarRow1.addView(createGhostButton("＋ 新規") { newFile() })
        toolbarRow1.addView(createGhostButton("📂 開く") { openFile() })
        toolbarRow1.addView(createDivider())
        toolbarRow1.addView(createAccentButton("💾 保存") { saveFile() })
        toolbarRow1.addView(createGhostButton("別名保存") { saveFileAs() })
        toolbarRow1.addView(createDivider())

        // エンコーディング選択スピナー
        encodingSpinner = Spinner(this).apply {
            val items = arrayOf("UTF-8", "Shift_JIS", "EUC-JP")
            val spinnerAdapter = object : ArrayAdapter<String>(this@MainActivity, android.R.layout.simple_spinner_item, items) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getView(position, convertView, parent) as TextView).apply {
                        setTextColor(colorText2)
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                        setBackgroundColor(colorSurface2)
                        setPadding(8.dp(), 4.dp(), 8.dp(), 4.dp())
                    }
                }
                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    return (super.getDropDownView(position, convertView, parent) as TextView).apply {
                        setTextColor(colorText)
                        setBackgroundColor(colorSurface)
                        setPadding(12.dp(), 8.dp(), 12.dp(), 8.dp())
                    }
                }
            }
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            adapter = spinnerAdapter
            val spinnerBg = GradientDrawable().apply {
                setColor(colorSurface2)
                cornerRadius = 7.dp().toFloat()
            }
            background = spinnerBg
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 36.dp()
            ).apply { marginStart = 4.dp() }
        }
        toolbarRow1.addView(encodingSpinner)

        toolbarRow1Scroll.addView(toolbarRow1)
        toolbarContainer.addView(toolbarRow1Scroll)

        // 行1下の区切り
        toolbarContainer.addView(View(this).apply {
            setBackgroundColor(colorBorder)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp())
        })

        // ツールバー行2
        val toolbarRow2Scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp()
            )
        }
        val toolbarRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10.dp(), 0, 10.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        toolbarRow2.addView(createIconButton("𝐁") { insertText("**太字**") })
        toolbarRow2.addView(createIconButton("𝐼") { insertText("*斜体*") })
        toolbarRow2.addView(createIconButton("H") { insertLine("# ") })
        toolbarRow2.addView(createIconButton("≡") { insertLine("- ") })
        toolbarRow2.addView(createIconButton("❝") { insertLine("> ") })
        toolbarRow2.addView(createIconButton("{ }") { insertText("```\n\n```") })

        toolbarRow2Scroll.addView(toolbarRow2)
        toolbarContainer.addView(toolbarRow2Scroll)

        // ツールバーコンテナ下の区切り
        toolbarContainer.addView(View(this).apply {
            setBackgroundColor(colorBorder)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp())
        })

        root.addView(toolbarContainer)

        // ===== エディター =====
        val editorScroll = ScrollView(this).apply {
            setBackgroundColor(colorBg)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            isFillViewport = true
        }

        val editorContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // 行番号エリア（左のガター）
        val lineNumberView = TextView(this).apply {
            setBackgroundColor(colorSurface)
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.END
            setPadding(8.dp(), 12.dp(), 8.dp(), 12.dp())
            text = "1"
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            minWidth = 36.dp()
        }

        // ガター区切り線
        val gutterDivider = View(this).apply {
            setBackgroundColor(colorBorder)
            layoutParams = LinearLayout.LayoutParams(1.dp(), ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // エディタEditText
        editorEditText = EditText(this).apply {
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(colorText)
            setHintTextColor(Color.argb(128, 160, 160, 184))
            hint = "ここに入力…　各種ファイルの読み書きに対応"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.MONOSPACE
            gravity = Gravity.TOP or Gravity.START
            setPadding(12.dp(), 12.dp(), 12.dp(), 12.dp())
            setLineSpacing(0f, 1.75f)
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            isSingleLine = false
            isVerticalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)

            // カーソル色はAPIレベルに依存するのでXMLリソースなしでは困難だが、テーマで対応
            highlightColor = Color.argb(64, 233, 69, 96)
        }

        editorEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!suppressTextWatcher) {
                    setModified(true)
                }
                updateStats()
                updateLineNumbers(lineNumberView)
            }
        })

        editorContainer.addView(lineNumberView)
        editorContainer.addView(gutterDivider)
        editorContainer.addView(editorEditText)
        editorScroll.addView(editorContainer)
        root.addView(editorScroll)

        // ===== ステータスバー =====
        val statusBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(colorSurface)
            setPadding(14.dp(), 0, 14.dp(), 0)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 28.dp()
            )
        }

        // 区切り線
        root.addView(View(this).apply {
            setBackgroundColor(colorBorder)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1.dp())
        })

        // 行数表示
        val linesLabel = TextView(this).apply {
            text = "📄 "
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        statLines = TextView(this).apply {
            text = "1"
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        val linesSuffix = TextView(this).apply {
            text = " 行"
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }

        statusBar.addView(linesLabel)
        statusBar.addView(statLines)
        statusBar.addView(linesSuffix)

        // スペーサー
        statusBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(14.dp(), 1)
        })

        // 文字数表示
        val charsLabel = TextView(this).apply {
            text = "✏️ "
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        statChars = TextView(this).apply {
            text = "0"
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        val charsSuffix = TextView(this).apply {
            text = " 文字"
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }

        statusBar.addView(charsLabel)
        statusBar.addView(statChars)
        statusBar.addView(charsSuffix)

        // スペーサー
        statusBar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(14.dp(), 1)
        })

        // ファイルタイプ表示
        statType = TextView(this).apply {
            text = "TXT"
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        }
        statusBar.addView(statType)

        root.addView(statusBar)

        setContentView(root)
        updateStats()
    }

    // ===== ボタン生成ヘルパー =====
    private fun createGhostButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAllCaps = false
            val bg = GradientDrawable().apply {
                setColor(colorBorder)
                cornerRadius = 7.dp().toFloat()
            }
            background = bg
            setPadding(11.dp(), 7.dp(), 11.dp(), 7.dp())
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = 4.dp() }
            setOnClickListener { onClick() }
        }
    }

    private fun createAccentButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAllCaps = false
            val bg = GradientDrawable().apply {
                setColor(colorAccent)
                cornerRadius = 7.dp().toFloat()
            }
            background = bg
            setPadding(13.dp(), 7.dp(), 13.dp(), 7.dp())
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = 4.dp() }
            setOnClickListener { onClick() }
        }
    }

    private fun createIconButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(colorText2)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            isAllCaps = false
            setBackgroundColor(Color.TRANSPARENT)
            setPadding(10.dp(), 8.dp(), 10.dp(), 8.dp())
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener { onClick() }
        }
    }

    private fun createDivider(): View {
        return View(this).apply {
            setBackgroundColor(colorBorder)
            layoutParams = LinearLayout.LayoutParams(1.dp(), 24.dp()).apply {
                marginStart = 4.dp()
                marginEnd = 4.dp()
            }
        }
    }

    // ===== 状態管理 =====
    private fun setModified(val_: Boolean) {
        isModified = val_
        modifiedDot.visibility = if (val_) View.VISIBLE else View.GONE
    }

    private fun updateStats() {
        val text = editorEditText.text.toString()
        val lineCount = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
        statLines.text = lineCount.toString()
        statChars.text = text.length.toString()
    }

    private fun updateLineNumbers(lineNumberView: TextView) {
        val text = editorEditText.text.toString()
        val lineCount = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
        val sb = StringBuilder()
        for (i in 1..lineCount) {
            sb.append(i)
            if (i < lineCount) sb.append("\n")
        }
        lineNumberView.text = sb.toString()
    }

    // ===== ファイル操作 =====
    private fun newFile() {
        if (isModified) {
            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("未保存の変更があります。破棄しますか？")
                .setPositiveButton("破棄") { _, _ ->
                    performNewFile()
                }
                .setNegativeButton("キャンセル", null)
                .show()
        } else {
            performNewFile()
        }
    }

    private fun performNewFile() {
        suppressTextWatcher = true
        editorEditText.setText("")
        suppressTextWatcher = false
        currentUri = null
        currentFileName = "新規ファイル"
        filenameDisplay.text = currentFileName
        statType.text = "TXT"
        setModified(false)
        updateStats()
        editorEditText.requestFocus()
    }

    private fun openFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        openFileLauncher.launch(intent)
    }

    private fun readFile(uri: Uri) {
        try {
            val charsetName = getSelectedCharset()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, charsetName))
                val text = reader.readText()
                reader.close()

                suppressTextWatcher = true
                editorEditText.setText(text)
                suppressTextWatcher = false

                currentUri = uri
                val name = getFileNameFromUri(uri)
                currentFileName = name
                filenameDisplay.text = name
                val ext = getExtension(name)
                statType.text = ext
                setModified(false)
                updateStats()
                Toast.makeText(this, "📂 ${name} を開きました ($charsetName)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ 開けませんでした: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFile() {
        if (currentUri == null) {
            saveFileAs()
            return
        }
        try {
            writeFile(currentUri!!)
            setModified(false)
            Toast.makeText(this, "✅ 保存しました", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "⚠️ 保存に失敗しました", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveFileAs() {
        val defaultName = if (currentFileName == "新規ファイル") "untitled.txt" else currentFileName
        val input = EditText(this).apply {
            setText(defaultName)
            setSelection(0, defaultName.lastIndexOf('.').let { if (it > 0) it else defaultName.length })
        }
        AlertDialog.Builder(this)
            .setTitle("ファイル名を入力")
            .setView(input)
            .setPositiveButton("保存") { _, _ ->
                val fileName = input.text.toString().ifBlank { "untitled.txt" }
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, fileName)
                }
                saveFileLauncher.launch(intent)
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun writeFile(uri: Uri) {
        val charsetName = getSelectedCharset()
        contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
            val writer = OutputStreamWriter(outputStream, charsetName)
            writer.write(editorEditText.text.toString())
            writer.flush()
            writer.close()
        }
    }

    // ===== インサート系 =====
    private fun insertText(text: String) {
        val start = editorEditText.selectionStart
        val end = editorEditText.selectionEnd
        editorEditText.text.replace(start.coerceAtLeast(0), end.coerceAtLeast(0), text)
        editorEditText.requestFocus()
    }

    private fun insertLine(prefix: String) {
        val start = editorEditText.selectionStart
        val content = editorEditText.text.toString()
        // 現在行の先頭を見つける
        var lineStart = start
        while (lineStart > 0 && content[lineStart - 1] != '\n') {
            lineStart--
        }
        editorEditText.text.insert(lineStart, prefix)
        editorEditText.requestFocus()
    }

    // ===== ユーティリティ =====
    private fun getExtension(filename: String): String {
        val dotIndex = filename.lastIndexOf('.')
        return if (dotIndex >= 0 && dotIndex < filename.length - 1) {
            filename.substring(dotIndex + 1).uppercase()
        } else {
            "FILE"
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getSelectedCharset(): String {
        return when (encodingSpinner.selectedItemPosition) {
            0 -> "UTF-8"
            1 -> "Shift_JIS"
            2 -> "EUC-JP"
            else -> "UTF-8"
        }
    }
}