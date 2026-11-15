package com.videotube.app

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videotube.app.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDb.get(app).dao()
    private val ctx get() = getApplication<Application>()

    val videos = dao.all().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val profile = dao.profile().map { it ?: ProfileEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProfileEntity())
    val saving = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private fun dir(n: String) = File(ctx.filesDir, n).apply { mkdirs() }

    private fun ext(uri: Uri, fallback: String): String {
        val t = ctx.contentResolver.getType(uri)
        return t?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) } ?: fallback
    }

    /** Streams in 256 KB chunks so very large videos never load into memory. */
    private fun copy(uri: Uri, dest: File) {
        val input = ctx.contentResolver.openInputStream(uri) ?: error("Cannot open file")
        input.use { i -> dest.outputStream().buffered(256 * 1024).use { o -> i.copyTo(o, 256 * 1024) } }
    }

    private fun scaled(b: Bitmap): Bitmap {
        val s = 720f / maxOf(b.width, b.height)
        return if (s >= 1f) b else Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
    }

    private fun probe(f: File): Pair<Long, Bitmap?> {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(f.absolutePath)
            val d = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bmp = r.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: r.getFrameAtTime(0)
            d to bmp?.let { scaled(it) }
        } catch (e: Exception) { 0L to null } finally { r.release() }
    }

    fun addVideo(uri: Uri, title: String, desc: String, category: String, thumb: Uri?, isShort: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            saving.value = true
            try {
                withContext(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    val vf = File(dir("videos"), "$now.${ext(uri, "mp4")}")
                    try { copy(uri, vf) } catch (e: Exception) { vf.delete(); throw e }
                    val (dur, frame) = probe(vf)
                    var tf: File? = null
                    if (thumb != null) {
                        tf = File(dir("thumbs"), "$now.${ext(thumb, "jpg")}"); copy(thumb, tf)
                    } else if (frame != null) {
                        tf = File(dir("thumbs"), "$now.jpg")
                        tf.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    }
                    val name = (displayName(ctx, uri) ?: "Video").substringBeforeLast('.')
                    dao.insert(
                        VideoEntity(
                            title = title.trim().ifBlank { name }, description = desc.trim(),
                            category = category.trim().ifBlank { "Other" },
                            videoPath = vf.absolutePath, thumbPath = tf?.absolutePath,
                            durationMs = dur, createdAt = now, isShort = isShort
                        )
                    )
                }
                onDone()
            } catch (e: Exception) {
                error.value = "Could not save video: ${e.message}"
            } finally { saving.value = false }
        }
    }

    fun updateVideo(v: VideoEntity, title: String, desc: String, category: String, isShort: Boolean, thumb: Uri?, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    var tp = v.thumbPath
                    if (thumb != null) {
                        val f = File(dir("thumbs"), "${System.currentTimeMillis()}.${ext(thumb, "jpg")}")
                        copy(thumb, f)
                        v.thumbPath?.let { File(it).delete() }
                        tp = f.absolutePath
                    }
                    dao.update(
                        v.copy(
                            title = title.trim().ifBlank { v.title }, description = desc.trim(),
                            category = category.trim().ifBlank { "Other" }, thumbPath = tp, isShort = isShort
                        )
                    )
                }
                onDone()
            } catch (e: Exception) { error.value = "Could not update: ${e.message}" }
        }
    }

    fun deleteVideo(v: VideoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            File(v.videoPath).delete()
            v.thumbPath?.let { File(it).delete() }
            dao.delete(v)
        }
    }

    fun saveProfile(name: String, bio: String, avatar: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cur = profile.value
                var path = cur.avatarPath
                if (avatar != null) {
                    val f = File(dir("avatars"), "${System.currentTimeMillis()}.${ext(avatar, "jpg")}")
                    copy(avatar, f)
                    cur.avatarPath?.let { File(it).delete() }
                    path = f.absolutePath
                }
                dao.saveProfile(cur.copy(username = name.trim().ifBlank { "Creator" }, bio = bio.trim(), avatarPath = path))
            } catch (e: Exception) { error.value = "Could not save profile: ${e.message}" }
        }
    }
}
