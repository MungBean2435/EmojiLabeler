package com.example.emojilabeler.data

import android.content.Context
import android.graphics.Color
import org.json.JSONArray

object SettingsStore {

    private const val PREFS = "settings"
    private const val KEY_EMOJI = "emoji_set"
    private const val KEY_RECENT = "recent_emojis"
    private const val KEY_COPY = "copy_mode"
    private const val KEY_AUTO = "auto_advance"
    private const val KEY_MULTI = "multi_label"
    private const val KEY_BG = "image_bg"

    val DEFAULT_EMOJI = listOf(
        "😂", "😭", "🤣", "😅", "😡", "😱", "🥰", "😍",
        "😴", "🙄", "😳", "👍", "👎", "❤️", "🔥", "🙏"
    )

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getEmojiSet(c: Context): List<String> {
        val s = prefs(c).getString(KEY_EMOJI, null) ?: return DEFAULT_EMOJI
        val arr = JSONArray(s)
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getString(i))
        return if (out.isEmpty()) DEFAULT_EMOJI else out
    }

    fun setEmojiSet(c: Context, list: List<String>) {
        prefs(c).edit().putString(KEY_EMOJI, JSONArray(list).toString()).apply()
    }

    fun getRecentEmojis(c: Context): List<String> {
        val s = prefs(c).getString(KEY_RECENT, null) ?: return emptyList()
        val arr = JSONArray(s)
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getString(i))
        return out
    }

    fun addRecentEmoji(c: Context, emoji: String) {
        val cur = getRecentEmojis(c).toMutableList()
        cur.remove(emoji)
        cur.add(0, emoji)
        prefs(c).edit().putString(KEY_RECENT, JSONArray(cur.take(12)).toString()).apply()
    }

    fun isCopyMode(c: Context) = prefs(c).getBoolean(KEY_COPY, true)
    fun setCopyMode(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_COPY, v).apply()

    fun isAutoAdvance(c: Context) = prefs(c).getBoolean(KEY_AUTO, true)
    fun setAutoAdvance(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_AUTO, v).apply()

    fun isMultiLabel(c: Context) = prefs(c).getBoolean(KEY_MULTI, false)
    fun setMultiLabel(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_MULTI, v).apply()

    /** 图片预览背景色，hex 字符串存储，null = 透明 */
    fun getImageBackgroundHex(c: Context): String? = prefs(c).getString(KEY_BG, null)

    fun getImageBackgroundColor(c: Context): Int {
        val hex = getImageBackgroundHex(c) ?: return Color.TRANSPARENT
        return try {
            Color.parseColor(hex)
        } catch (_: Exception) {
            Color.TRANSPARENT
        }
    }

    fun setImageBackgroundColor(c: Context, hex: String) {
        prefs(c).edit().putString(KEY_BG, hex).apply()
    }

    /** 候选 emoji 大全（添加弹窗里展示） */
    val CANDIDATES = listOf(
        "😀","😃","😄","😁","😆","😅","🤣","😂","🙂","🙃","😉","😊","😇","🥰","😍","🤩","😘","😗","😚","😙",
        "🥲","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","🤨","😐","😑","😶","😏","😒","🙄","😬",
        "😌","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🥵","🥶","🥴","😵","🤯","🤠","🥳","🥸","😎","🤓",
        "🧐","😕","😟","🙁","😮","😯","😲","😳","🥺","🥹","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣",
        "😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","💩","🤡","👻","👽","🤖","🎃","😺","😸",
        "😹","😻","😼","😽","🙀","😿","😾","👍","👎","👌","🤌","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇",
        "☝️","✋","🖐️","🖖","👋","🤚","👏","🙌","🤲","🤝","🙏","💪","👊","✊","🤛","🤜","❤️","🧡","💛","💚",
        "💙","💜","🖤","🤍","🤎","💔","💕","💞","💓","💗","💖","💘","💝","💟","✨","⭐","🌟","💫","💯","🎉",
        "🎊","🎈","🎁","🏆","🥇","🥈","🥉","🍺","🍻","🥂","☕","🍵","🍰","🎂","🍭","🍩","🍔","🍟","🍕","🍀",
        "🌹","🌻","🌈","☀️","🌙","⚡","💧","❄️","🆗","🆒","🆕","🆙","🆓","💤","❗","❓","❌","✅","⚠️","🚫",
        "💢","💥","💦","💨","📱","💻","🎮","🎧","🎵","🎶","💡","🔋","💾","📷","📞","💬","💭","👀","🫠","🫨",
        "🫣","🫢","🫧","🫦","🫡","🫰","🫱","🫲","🫳","🫴","🫵","🫶","🪿","🪼","🪻","🪷","🪺","🪸","🪭","🪮"
    ).distinct()
}