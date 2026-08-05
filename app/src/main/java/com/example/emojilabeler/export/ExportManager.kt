package com.example.emojilabeler.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.emojilabeler.data.ImageItem
import org.json.JSONObject
import java.io.File

object ExportManager {

    /**
     * 导出：把已标注图片按 emoji 复制到用户选择的目录，生成
     *   <emoji>/图片  子文件夹 + labels.json + stats.json
     * 多标签的图片会复制进多个 emoji 文件夹（训练时每个类别各算一个样本）。
     * 返回导出的图片张数。onProgress(done, total) 在后台线程回调。
     */
    fun export(
        context: Context,
        treeUri: Uri,
        items: List<ImageItem>,
        onProgress: (Int, Int) -> Unit
    ): Int {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return 0
        val labeled = items.filter { it.status == ImageItem.STATUS_LABELED && it.labels.isNotEmpty() }
        val stats = sortedMapOf<String, Int>()
        var exported = 0

        for ((i, item) in labeled.withIndex()) {
            val uniqueLabels = item.labels.distinct()
            for (emoji in uniqueLabels) {
                val dir = root.findFile(emoji) ?: root.createDirectory(emoji) ?: continue
                val outName = uniqueName(dir, item.fileName)
                val outFile = dir.createFile(mimeOf(item.fileName), outName) ?: continue
                try {
                    context.contentResolver.openInputStream(uriOf(item.source))?.use { input ->
                        context.contentResolver.openOutputStream(outFile.uri)?.use { output ->
                            input.copyTo(output)
                        }
                    }
                    stats[emoji] = (stats[emoji] ?: 0) + 1
                    exported++
                } catch (_: Exception) {
                }
            }
            onProgress(i + 1, labeled.size)
        }

        // labels.json: {"0":"😂","1":"😭",...}   stats.json: {"😂":123,...}
        val classes = stats.keys.sorted()
        val labelsObj = JSONObject()
        classes.forEachIndexed { idx, c -> labelsObj.put(idx.toString(), c) }
        writeFile(context, root, "labels.json", labelsObj.toString(2))
        val statsObj = JSONObject()
        for ((k, v) in stats) statsObj.put(k, v)
        writeFile(context, root, "stats.json", statsObj.toString(2))

        return exported
    }

    private fun uniqueName(dir: DocumentFile, name: String): String {
        var n = name
        var i = 1
        while (dir.findFile(n) != null) {
            val dot = name.lastIndexOf('.')
            n = if (dot > 0) name.substring(0, dot) + "_$i" + name.substring(dot) else name + "_$i"
            i++
        }
        return n
    }

    private fun mimeOf(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "bmp" -> "image/bmp"
        else -> "image/jpeg"
    }

    private fun uriOf(source: String): Uri =
        if (source.startsWith("content://")) Uri.parse(source)
        else Uri.fromFile(File(Uri.parse(source).path ?: source))

    private fun writeFile(context: Context, root: DocumentFile, name: String, content: String) {
        root.findFile(name)?.delete()
        val f = root.createFile("application/json", name) ?: return
        context.contentResolver.openOutputStream(f.uri)?.use { it.write(content.toByteArray()) }
    }
}