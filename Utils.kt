package com.videotube.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import android.text.format.DateUtils
import com.videotube.app.data.VideoEntity

fun formatDuration(ms: Long): String {
    val s = ms / 1000
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, sec) else "%d:%02d".format(m, sec)
}

fun relativeDate(ts: Long): String =
    DateUtils.getRelativeTimeSpanString(ts, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

fun displayName(ctx: Context, uri: Uri): String? = try {
    ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }
} catch (e: Exception) { null }

fun Context.findActivity(): Activity {
    var c = this
    while (c is ContextWrapper) { if (c is Activity) return c; c = c.baseContext }
    error("No activity")
}

fun VideoEntity.matches(q: String): Boolean {
    val t = q.trim()
    return t.isEmpty() || title.contains(t, true) || category.contains(t, true)
}

val DEFAULT_CATEGORIES = listOf("Music", "Gaming", "Education", "Entertainment", "Travel", "Sports", "Vlog", "Other")
