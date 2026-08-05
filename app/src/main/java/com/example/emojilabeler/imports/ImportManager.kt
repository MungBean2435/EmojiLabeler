package com.example.emojilabeler.imports

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.emojilabeler.data.ImageItem
import java.io.File
import java.io.FileOutputStream

object ImportManager {

    private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp")

    private fun scanTree(root: DocumentFile, depth: Int, out: MutableList<DocumentFile>) {
        if (depth > 4) return
        for (f in root.listFiles()) {
            if (f.isDirectory) {
                scanTree(f, depth + 1, out)
            } else if (f.isFile) {
                val ext = f.name?.substringAfterLast('.', "")?.lowercase()
                if (ext in IMAGE_EXTS) out.add(f)
            }
        }
    }

    /**
     * 导入：扫描 SAF 文档树下的所有图片。
     * copyMode=true  复制到 App 私有目录 filesDir/workspace（稳定，推荐）
     * copyMode=false 保留在原文件夹，source 存 content:// Uri（省空间）
     * onProgress(done, total) 在后台线程回调。
     */
    fun importTree(
        context: Context,
        treeUri: Uri,
        copyMode: Boolean,
        onProgress: (Int, Int) -> Unit
    ): List<ImageItem> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val files = mutableListOf<DocumentFile>()
        scanTree(root, 0, files)

        val dir = File(context.filesDir, "workspace").apply { mkdirs() }
        val items = mutableListOf<ImageItem>()
        var id = System.currentTimeMillis()

        for ((i, f) in files.withIndex()) {
            val name = f.name ?: "img_$i.jpg"
            try {
                if (copyMode) {
                    val target = File(dir, "${i}_$name")
                    context.contentResolver.openInputStream(f.uri)?.use { input ->
                        FileOutputStream(target).use { out -> input.copyTo(out) }
                    }
                    items.add(ImageItem(id++, name, target.toURI().toString()))
                } else {
                    items.add(ImageItem(id++, name, f.uri.toString()))
                }
            } catch (_: Exception) {
                // 跳过无法读取的文件
            }
            onProgress(i + 1, files.size)
        }
        return items
    }
}