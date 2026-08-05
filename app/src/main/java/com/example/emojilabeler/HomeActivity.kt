package com.example.emojilabeler

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.emojilabeler.data.SettingsStore
import com.example.emojilabeler.data.StateStore
import com.example.emojilabeler.export.ExportManager
import com.example.emojilabeler.imports.ImportManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class HomeActivity : AppCompatActivity() {

    private lateinit var state: StateStore
    private lateinit var tvStats: TextView

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) doImport(uri) else toast("已取消导入") }

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri -> if (uri != null) doExport(uri) else toast("已取消导出") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        state = StateStore(this).apply { load() }
        tvStats = findViewById(R.id.tvStats)

        findViewById<Button>(R.id.btnContinue).setOnClickListener {
            startActivity(Intent(this, LabelActivity::class.java))
        }
        findViewById<Button>(R.id.btnImport).setOnClickListener { importLauncher.launch(null) }
        findViewById<Button>(R.id.btnExport).setOnClickListener {
            if (state.items.isEmpty()) {
                toast("请先导入图片")
                return@setOnClickListener
            }
            exportLauncher.launch(null)
        }
        findViewById<Button>(R.id.btnReview).setOnClickListener {
            startActivity(Intent(this, ReviewActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<TextView>(R.id.btnClear).setOnClickListener { confirmClear() }
    }

    override fun onResume() {
        super.onResume()
        state.load()
        refreshStats()
    }

    private fun refreshStats() {
        val (total, labeled, skipped) = state.stats()
        val pending = total - labeled - skipped
        tvStats.text = if (total == 0) {
            "还没有导入图片\n先去「导入新文件夹」选择表情包文件夹"
        } else {
            "共 $total 张 ｜ 已标 $labeled ｜ 待标 $pending ｜ 跳过 $skipped"
        }
    }

    private fun confirmClear() {
        if (state.items.isEmpty()) {
            toast("当前没有数据")
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("清空全部数据？")
            .setMessage(
                "将删除已导入的全部图片和标注状态，不可恢复。\n\n" +
                    "复制模式下 App 内的图片也会一并删除；\n" +
                    "如果只是想换文件夹，清空后重新「导入新文件夹」即可。"
            )
            .setPositiveButton("清空") { _, _ ->
                state.clear()
                File(filesDir, "workspace").deleteRecursively()
                refreshStats()
                toast("已清空，可以重新导入正确的文件夹")
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doImport(treeUri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        val copyMode = SettingsStore.isCopyMode(this)
        val dialog = ProgressDialog(this).apply {
            setMessage("正在扫描和导入…")
            setCancelable(false)
            show()
        }
        Thread {
            val newItems = ImportManager.importTree(this, treeUri, copyMode) { done, total ->
                runOnUiThread { dialog.setMessage("正在导入 $done / $total") }
            }
            runOnUiThread {
                dialog.dismiss()
                if (newItems.isEmpty()) {
                    toast("没有找到图片，请选择包含 jpg/png/webp/gif 的文件夹")
                    return@runOnUiThread
                }
                state.replaceItems(newItems)
                state.save()
                toast("导入完成：${newItems.size} 张")
                startActivity(Intent(this, LabelActivity::class.java))
            }
        }.start()
    }

    private fun doExport(treeUri: Uri) {
        val dialog = ProgressDialog(this).apply {
            setMessage("正在导出…")
            setCancelable(false)
            show()
        }
        Thread {
            val count = ExportManager.export(this, treeUri, state.items) { done, total ->
                runOnUiThread { dialog.setMessage("正在导出 $done / $total") }
            }
            runOnUiThread {
                dialog.dismiss()
                if (count > 0) toast("导出完成：$count 张\n已按 emoji 分类并生成 labels.json / stats.json")
                else toast("没有已标注的图片可导出")
            }
        }.start()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}