package com.example.emojilabeler

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.emojilabeler.data.ImageItem
import com.example.emojilabeler.data.SettingsStore
import com.example.emojilabeler.data.StateStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ReviewActivity : AppCompatActivity() {

    private lateinit var state: StateStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review)
        state = StateStore(this).apply { load() }

        val rv = findViewById<RecyclerView>(R.id.reviewList)
        rv.layoutManager = GridLayoutManager(this, 3)
        val labeled = state.items.filter { it.status == ImageItem.STATUS_LABELED }
        rv.adapter = ReviewAdapter(labeled) { item -> showEditDialog(item) }
        if (labeled.isEmpty()) {
            findViewById<TextView>(R.id.tvEmpty).apply {
                text = "还没有已标注的图片"
                visibility = View.VISIBLE
            }
        }
    }

    private fun showEditDialog(item: ImageItem) {
        val scroll = ScrollView(this)
        val grid = GridLayout(this).apply {
            columnCount = 4
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        val emojis = SettingsStore.getEmojiSet(this)
        for (e in emojis) {
            val tv = TextView(this).apply {
                text = e
                textSize = 24f
                gravity = Gravity.CENTER
                background = rippleBg()
                isClickable = true
                setOnClickListener {
                    item.labels.clear()
                    item.labels.add(e)
                    item.status = ImageItem.STATUS_LABELED
                    state.save()
                    recreate()
                }
            }
            grid.addView(tv, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(48)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            })
        }
        scroll.addView(grid)
        MaterialAlertDialogBuilder(this)
            .setTitle("改标：${item.fileName}")
            .setView(scroll)
            .setNegativeButton("清除标签") { _, _ ->
                item.labels.clear()
                item.status = ImageItem.STATUS_PENDING
                state.save()
                recreate()
            }
            .setNeutralButton("取消", null)
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