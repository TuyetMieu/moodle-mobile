package com.example.moodle_usth_mobile_app.data.local

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.moodle_usth_mobile_app.data.remote.DownloadedFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

object DownloadSaver {

    /** Lưu file vào thư mục Download và trả về mô tả vị trí để hiển thị cho người dùng. */
    suspend fun save(context: Context, file: DownloadedFile): String = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.fileName)
                put(MediaStore.Downloads.MIME_TYPE, file.mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Không tạo được file trong thư mục Download")
            resolver.openOutputStream(uri)?.use { it.write(file.bytes) }
                ?: throw IOException("Không ghi được file")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Download/${file.fileName}"
        } else {
            val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val target = File(directory, file.fileName)
            target.writeBytes(file.bytes)
            target.absolutePath
        }
    }
}
