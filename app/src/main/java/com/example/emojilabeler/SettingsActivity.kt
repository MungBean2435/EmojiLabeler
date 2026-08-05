package com.example.emojilabeler

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.emojilabeler.data.SettingsStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsActivity : AppCompatActivity() {

    private val currentSet = SettingsStore.DEFAULT_EMOJI.toMutableList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        currentSet.clear()
        currentSet.addAll(SettingsStore.getEmojiSet(this))
        renderEmojiGrid()

        val rg = findViewById<RadioGroup>(R.id.rgStorage)
        rg.check(if (SettingsStore.isCopyMode(this)) R.id.rbCopy else R.id.rbInplace)

        val swAuto = findViewById<Switch>(R.id.swAuto).apply {
            isChecked = SettingsStore.isAutoAdvance(this@SettingsActivity)
        }
        val swMulti = findViewById<Switch>(R.id.swMulti).apply {
            isChecked = SettingsStore.isMultiLabel(this@SettingsActivity)
        }

        // 图片背景
        val rgBg = findViewById<RadioGroup>(R.id.rgBg)
        val etBg = findViewById<EditText>(R.id.etBgCustom)
        when (SettingsStore.getImageBackgroundColor(this)) {
            Color.BLACK -> rgBg.check(R.id.rbBgBlack)
            Color.WHITE -> rgBg.check(R.id.rbBgWhite)
            else -> rgBg.check(R.id.rbBgTransparent)
        }
        val savedHex = SettingsStore.getImageBackgroundHex(this)
        if (savedHex != null && savedHex.uppercase() !in setOf("#00000000", "#FF000000", "#FFFFFFFF")) {
            etBg.setText(savedHex)
        }

        findViewById<Button>(R.id.btnAddEmoji).setOnClickListener { showAddDialog() }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            SettingsStore.setEmojiSet(this, currentSet)
            SettingsStore.setCopyMode(this, rg.checkedRadioButtonId == R.id.rbCopy)
            SettingsStore.setAutoAdvance(this, swAuto.isChecked)
            SettingsStore.setMultiLabel(this, swMulti.isChecked)

            // 背景色：自定义输入优先，其次单选
            val custom = etBg.text.toString().trim()
            var bgHex: String? = null
            if (custom.isNotEmpty()) {
                try {
                    Color.parseColor(custom)
                    bgHex = custom
                } catch (_: Exception) {
                    Toast.makeText(this, "背景颜色格式不对，已忽略自定义值", Toast.LENGTH_SHORT).show()
                }
            }
            if (bgHex == null) {
                bgHex = when (rgBg.checkedRadioButtonId) {
                    R.id.rbBgBlack -> "#FF000000"
                    R.id.rbBgWhite -> "#FFFFFFFF"
                    else -> "#00000000"
                }
            }
            SettingsStore.setImageBackgroundColor(this, bgHex)

            Toast.makeText(this, "已保存 ✓", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun renderEmojiGrid() {
        val grid = findViewById<GridLayout>(R.id.emojiGrid)
        grid.removeAllViews()
        grid.columnCount = 4
        for (e in currentSet) {
            val tv = TextView(this).apply {
                text = e
                textSize = 24f
                gravity = Gravity.CENTER
                background = rippleBg()
                setOnLongClickListener {
                    MaterialAlertDialogBuilder(this@SettingsActivity)
                        .setTitle("删除 $e ？")
                        .setMessage("删除后标注页不再显示这个 emoji（已标的数据不受影响）")
                        .setPositiveButton("删除") { _, _ ->
                            currentSet.remove(e)
                            renderEmojiGrid()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                    true
                }
            }
            grid.addView(tv, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(48)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            })
        }
    }

    private fun showAddDialog() {
        val scroll = ScrollView(this)
        val ll = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val et = EditText(this).apply {
            hint = "也可以直接粘贴任意 emoji"
        }
        val btnAdd = Button(this).apply { text = "＋ 添加这个" }
        btnAdd.setOnClickListener {
            val t = et.text.toString().trim()
            if (t.isNotEmpty() && t !in currentSet) {
                currentSet.add(t)
                renderEmojiGrid()
                Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show()
            }
        }
        ll.addView(et)
        ll.addView(btnAdd)

        val grid = GridLayout(this).apply { columnCount = 4 }
        for (e in SettingsStore.CANDIDATES) {
            if (e in currentSet) continue
            val tv = TextView(this).apply {
                text = e
                textSize = 22f
                gravity = Gravity.CENTER
                background = rippleBg()
                setOnClickListener {
                    if (e !in currentSet) {
                        currentSet.add(e)
                        renderEmojiGrid()
                        Toast.makeText(this@SettingsActivity, "已添加 $e", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            grid.addView(tv, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(44)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            })
        }
        ll.addView(grid)
        scroll.addView(ll)

        MaterialAlertDialogBuilder(this)
            .setTitle("添加 emoji")
            .setView(scroll)
            .setPositiveButton("完成", null)
            .show()
    }

    private fun rippleBg(): RippleDrawable {
        val bg = GradientDrawable().apply {
            setColor(Color.rgb(242, 242, 242))
            cornerRadius = dp(8).toFloat()
        }
        return RippleDrawable(ColorStateList.valueOf(Color.rgb(187, 187, 187)), bg, null)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}