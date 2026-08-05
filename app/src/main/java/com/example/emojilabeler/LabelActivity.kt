package com.example.emojilabeler

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.emojilabeler.data.ImageItem
import com.example.emojilabeler.data.SettingsStore
import com.example.emojilabeler.data.StateStore

class LabelActivity : AppCompatActivity() {

    private lateinit var state: StateStore
    private var pos = 0
    private var zoomed = false

    private lateinit var imageView: ImageView
    private lateinit var tvProgress: TextView
    private lateinit var tvFilename: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var emojiGrid: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_label)

        state = StateStore(this).apply { load() }
        if (state.items.isEmpty()) { finish(); return }

        imageView = findViewById(R.id.imageView)
        tvProgress = findViewById(R.id.tvProgress)
        tvFilename = findViewById(R.id.tvFilename)
        progressBar = findViewById(R.id.progressBar)
        emojiGrid = findViewById(R.id.emojiGrid)

        pos = savedInstanceState?.getInt("pos", 0) ?: firstPending()

        findViewById<Button>(R.id.btnUndo).setOnClickListener { undoCurrent() }
        findViewById<Button>(R.id.btnPrev).setOnClickListener { move(-1) }
        findViewById<Button>(R.id.btnSkip).setOnClickListener { skipCurrent() }
        findViewById<Button>(R.id.btnNext).setOnClickListener { move(1) }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        imageView.setOnClickListener { toggleZoom() }

        buildEmojiGrid()
        buildQuickRow()
        applyImageBackground()
        showCurrent()
    }

    override fun onResume() {
        super.onResume()
        if (state.items.isEmpty()) return
        // 从设置页返回后刷新：emoji 集合/背景可能被修改
        buildEmojiGrid()
        buildQuickRow()
        applyImageBackground()
        showCurrent()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("pos", pos)
    }

    override fun onPause() {
        super.onPause()
        state.save()
    }

    // ---------- 核心操作 ----------

    private fun onEmojiClick(emoji: String) {
        val item = state.items.getOrNull(pos) ?: return
        SettingsStore.addRecentEmoji(this, emoji)
        if (SettingsStore.isMultiLabel(this)) {
            if (emoji !in item.labels) {
                item.labels.add(emoji)
                item.status = ImageItem.STATUS_LABELED
                state.save()
            }
            showCurrent()
        } else {
            item.labels.clear()
            item.labels.add(emoji)
            item.status = ImageItem.STATUS_LABELED
            state.save()
            if (SettingsStore.isAutoAdvance(this)) {
                val next = nextPending(pos)
                if (next >= 0) { pos = next; showCurrent() } else doneToast()
            } else {
                showCurrent()
            }
        }
    }

    private fun undoCurrent() {
        val item = state.items.getOrNull(pos) ?: return
        if (item.labels.isNotEmpty() || item.status == ImageItem.STATUS_LABELED) {
            item.labels.clear()
            item.status = ImageItem.STATUS_PENDING
            state.save()
        }
        showCurrent()
    }

    private fun skipCurrent() {
        val item = state.items.getOrNull(pos) ?: return
        item.labels.clear()
        item.status = ImageItem.STATUS_SKIPPED
        state.save()
        val next = nextPending(pos)
        if (next >= 0) { pos = next; showCurrent() } else doneToast()
    }

    private fun move(delta: Int) {
        var p = pos + delta
        if (p < 0) p = 0
        if (p >= state.items.size) p = state.items.size - 1
        pos = p
        showCurrent()
    }

    private fun firstPending(): Int {
        for (i in state.items.indices) if (state.items[i].status == ImageItem.STATUS_PENDING) return i
        return 0
    }

    private fun nextPending(from: Int): Int {
        val n = state.items.size
        for (i in 1..n) {
            val idx = (from + i) % n
            if (state.items[idx].status == ImageItem.STATUS_PENDING) return idx
        }
        return -1
    }

    private fun doneToast() {
        Toast.makeText(this, "🎉 全部标完啦！回首页导出吧", Toast.LENGTH_LONG).show()
    }

    // ---------- 界面 ----------

    private fun showCurrent() {
        if (pos !in state.items.indices) pos = 0
        val item = state.items[pos]
        imageView.load(Uri.parse(item.source)) { crossfade(true) }
        tvFilename.text = item.fileName
        tvProgress.text = "${pos + 1} / ${state.items.size}"
        val (total, labeled, _) = state.stats()
        progressBar.max = total.coerceAtLeast(1)
        progressBar.progress = labeled
        supportActionBar?.subtitle =
            if (item.labels.isEmpty()) "" else "已标: " + item.labels.joinToString(" ")
    }

    private fun buildEmojiGrid() {
        emojiGrid.removeAllViews()
        emojiGrid.columnCount = 4
        val emojis = SettingsStore.getEmojiSet(this)
        for (e in emojis) {
            val btn = TextView(this).apply {
                text = e
                textSize = 26f
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                background = rippleBg()
                setOnClickListener { onEmojiClick(e) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(52)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f)
            }
            emojiGrid.addView(btn, lp)
        }
    }

    private fun buildQuickRow() {
        val rv = findViewById<RecyclerView>(R.id.quickRow)
        val recent = SettingsStore.getRecentEmojis(this)
            .filter { it in SettingsStore.getEmojiSet(this) }
        if (recent.isEmpty()) {
            rv.visibility = View.GONE
            findViewById<TextView>(R.id.tvQuickLabel).visibility = View.GONE
            return
        }
        rv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun getItemCount() = recent.size
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val tv = TextView(this@LabelActivity).apply {
                    textSize = 24f
                    gravity = Gravity.CENTER
                    layoutParams = ViewGroup.LayoutParams(dp(52), dp(52))
                }
                return object : RecyclerView.ViewHolder(tv) {}
            }
            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val tv = holder.itemView as TextView
                tv.text = recent[position]
                tv.background = rippleBg()
                tv.setOnClickListener { onEmojiClick(recent[position]) }
            }
        }
    }

    private fun rippleBg(): RippleDrawable {
        val bg = GradientDrawable().apply {
            setColor(Color.rgb(242, 242, 242))
            cornerRadius = dp(10).toFloat()
        }
        return RippleDrawable(ColorStateList.valueOf(Color.rgb(187, 187, 187)), bg, null)
    }

    private fun toggleZoom() {
        zoomed = !zoomed
        imageView.animate()
            .scaleX(if (zoomed) 2f else 1f)
            .scaleY(if (zoomed) 2f else 1f)
            .setDuration(150)
            .start()
    }

    private fun applyImageBackground() {
        imageView.setBackgroundColor(SettingsStore.getImageBackgroundColor(this))
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}