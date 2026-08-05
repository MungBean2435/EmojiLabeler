package com.example.emojilabeler.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ImageItem(
    val id: Long,
    val fileName: String,
    val source: String,               // content://... 或 file://...
    val labels: MutableList<String> = mutableListOf(),
    var status: String = ImageItem.STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_LABELED = "LABELED"
        const val STATUS_SKIPPED = "SKIPPED"
    }
}

class StateStore(private val context: Context) {

    private val file = File(context.filesDir, "state.json")

    var items: MutableList<ImageItem> = mutableListOf()
        private set

    fun load() {
        items = mutableListOf()
        if (!file.exists()) return
        try {
            val root = JSONObject(file.readText())
            val arr = root.getJSONArray("items")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val labels = mutableListOf<String>()
                val la = o.optJSONArray("labels")
                if (la != null) for (j in 0 until la.length()) labels.add(la.getString(j))
                items.add(ImageItem(
                    id = o.getLong("id"),
                    fileName = o.getString("fileName"),
                    source = o.getString("source"),
                    labels = labels,
                    status = o.getString("status")
                ))
            }
        } catch (_: Exception) {
            items = mutableListOf()
        }
    }

    fun save() {
        val arr = JSONArray()
        for (it in items) {
            val o = JSONObject()
            o.put("id", it.id)
            o.put("fileName", it.fileName)
            o.put("source", it.source)
            o.put("labels", JSONArray(it.labels))
            o.put("status", it.status)
            arr.put(o)
        }
        val root = JSONObject()
        root.put("items", arr)
        file.writeText(root.toString())
    }

    fun replaceItems(list: List<ImageItem>) {
        items = list.toMutableList()
    }

    /** 清空全部数据：内存列表 + 删除 state.json（复制模式的图片文件由调用方删除） */
    fun clear() {
        items = mutableListOf()
        file.delete()
    }

    /** 返回 (总数, 已标, 跳过) */
    fun stats(): Triple<Int, Int, Int> {
        var labeled = 0
        var skipped = 0
        for (it in items) when (it.status) {
            ImageItem.STATUS_LABELED -> labeled++
            ImageItem.STATUS_SKIPPED -> skipped++
        }
        return Triple(items.size, labeled, skipped)
    }
}