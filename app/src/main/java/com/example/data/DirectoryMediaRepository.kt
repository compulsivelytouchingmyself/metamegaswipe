package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.exifinterface.media.ExifInterface
import com.example.model.ImageItem
import com.example.model.TaggingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

sealed interface TagWriteResult {
    data class Success(val tags: List<String>) : TagWriteResult
    data class Skipped(val tags: List<String>) : TagWriteResult
    data class Error(val message: String) : TagWriteResult
}

class DirectoryMediaRepository(private val context: Context) {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif")

    suspend fun getFolderDisplayName(treeUri: Uri): String = withContext(Dispatchers.IO) {
        val doc = DocumentFile.fromTreeUri(context, treeUri)
        doc?.name?.takeIf { it.isNotBlank() } ?: treeUri.lastPathSegment?.substringAfterLast(':') ?: "Selected Folder"
    }

    suspend fun scanDirectory(treeUri: Uri): List<ImageItem> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        if (!root.exists() || !root.isDirectory) return@withContext emptyList()

        root.listFiles()
            .asSequence()
            .filter { file ->
                if (!file.isFile) return@filter false
                val name = file.name ?: return@filter false
                val mime = file.type?.lowercase() ?: ""
                val ext = name.substringAfterLast('.', "").lowercase()
                mime.startsWith("image/") || ext in imageExtensions
            }
            .map { file ->
                val name = file.name ?: "image"
                val stem = name.substringBeforeLast('.', name)
                val mime = file.type ?: guessMimeFromExtension(name)
                ImageItem(
                    uri = file.uri,
                    name = name,
                    stem = stem,
                    mimeType = mime,
                    sizeBytes = file.length(),
                )
            }
            .sortedWith(naturalFileNameComparator)
            .toList()
    }

    suspend fun readTags(
        treeUri: Uri,
        item: ImageItem,
        mode: TaggingMode,
    ): List<String> = withContext(Dispatchers.IO) {
        when (mode) {
            TaggingMode.DATASET_SIDECAR -> readSidecarTags(treeUri, item.stem)
            TaggingMode.HEADER_METADATA -> readHeaderTags(item.uri)
        }
    }

    suspend fun appendTag(
        treeUri: Uri,
        item: ImageItem,
        rawInput: String,
        mode: TaggingMode,
    ): TagWriteResult = withContext(Dispatchers.IO) {
        val incomingTags = parseTagString(rawInput)
        val existingTags = when (mode) {
            TaggingMode.DATASET_SIDECAR -> readSidecarTags(treeUri, item.stem)
            TaggingMode.HEADER_METADATA -> readHeaderTags(item.uri)
        }

        if (incomingTags.isEmpty()) {
            return@withContext TagWriteResult.Skipped(existingTags)
        }

        val merged = mergeTags(existingTags, incomingTags)
        writeTagsInternal(treeUri, item, merged, mode)
    }

    suspend fun replaceTags(
        treeUri: Uri,
        item: ImageItem,
        newTags: List<String>,
        mode: TaggingMode,
    ): TagWriteResult = withContext(Dispatchers.IO) {
        val cleaned = newTags.map { it.trim() }.filter { it.isNotEmpty() }.distinctBy { it.lowercase() }
        writeTagsInternal(treeUri, item, cleaned, mode)
    }

    suspend fun deleteImage(
        treeUri: Uri,
        item: ImageItem,
        deleteSidecar: Boolean = true,
    ): Boolean = withContext(Dispatchers.IO) {
        val deleted = runCatching {
            DocumentsContract.deleteDocument(context.contentResolver, item.uri)
        }.getOrElse {
            DocumentFile.fromSingleUri(context, item.uri)?.delete() == true
        }

        if (deleted && deleteSidecar) {
            runCatching {
                val root = DocumentFile.fromTreeUri(context, treeUri)
                val sidecar = findSidecarFile(root, item.stem)
                sidecar?.delete()
            }
        }
        deleted
    }

    private fun writeTagsInternal(
        treeUri: Uri,
        item: ImageItem,
        tags: List<String>,
        mode: TaggingMode,
    ): TagWriteResult {
        return when (mode) {
            TaggingMode.DATASET_SIDECAR -> writeSidecarTags(treeUri, item.stem, tags)
            TaggingMode.HEADER_METADATA -> writeHeaderTags(item, tags)
        }
    }

    private fun readSidecarTags(treeUri: Uri, stem: String): List<String> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val sidecar = findSidecarFile(root, stem) ?: return emptyList()
        return runCatching {
            context.contentResolver.openInputStream(sidecar.uri)?.use { input ->
                BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
            }.orEmpty()
        }.map { parseTagString(it) }.getOrDefault(emptyList())
    }

    private fun writeSidecarTags(treeUri: Uri, stem: String, tags: List<String>): TagWriteResult {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: return TagWriteResult.Error("Cannot access working directory.")

        val targetName = "$stem.txt"
        var sidecar = findSidecarFile(root, stem)
        if (sidecar == null) {
            sidecar = root.createFile("text/plain", stem)
            if (sidecar != null && sidecar.name != targetName) {
                runCatching { sidecar.renameTo(targetName) }
                sidecar = findSidecarFile(root, stem) ?: sidecar
            }
        }

        if (sidecar == null) {
            return TagWriteResult.Error("Unable to create $targetName in folder.")
        }

        val payload = tags.joinToString(", ")
        return runCatching {
            context.contentResolver.openOutputStream(sidecar.uri, "wt")?.use { out ->
                out.write(payload.toByteArray(StandardCharsets.UTF_8))
                out.flush()
            } ?: error("Output stream unavailable")
            TagWriteResult.Success(tags)
        }.getOrElse { err ->
            TagWriteResult.Error("Failed to write $targetName: ${err.localizedMessage ?: "I/O error"}")
        }
    }

    private fun readHeaderTags(imageUri: Uri): List<String> {
        return runCatching {
            context.contentResolver.openFileDescriptor(imageUri, "r")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                val desc = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION).orEmpty()
                val comment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT).orEmpty()
                val raw = desc.ifBlank { comment }
                parseTagString(raw)
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun writeHeaderTags(item: ImageItem, tags: List<String>): TagWriteResult {
        if (!ExifInterface.isSupportedMimeType(item.mimeType)) {
            return TagWriteResult.Error(
                "Format (${item.mimeType.substringAfter('/')}) does not support EXIF header writes. Switch to Dataset Mode for .txt sidecars.",
            )
        }
        val payload = tags.joinToString(", ")
        return runCatching {
            val pfd = context.contentResolver.openFileDescriptor(item.uri, "rw")
                ?: return TagWriteResult.Error("Storage provider denied read-write access for ${item.name}.")
            pfd.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, payload)
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, payload)
                exif.saveAttributes()
            }
            TagWriteResult.Success(tags)
        }.getOrElse { err ->
            TagWriteResult.Error("Metadata write failed for ${item.name}: ${err.localizedMessage ?: "Unsupported file"}")
        }
    }

    private fun findSidecarFile(root: DocumentFile?, stem: String): DocumentFile? {
        if (root == null) return null
        val exact = root.findFile("$stem.txt")
        if (exact != null && exact.isFile) return exact
        return root.listFiles().firstOrNull {
            it.isFile && it.name.equals("$stem.txt", ignoreCase = true)
        }
    }

    private fun parseTagString(raw: String): List<String> {
        return raw.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun mergeTags(existing: List<String>, incoming: List<String>): List<String> {
        val result = existing.toMutableList()
        val seenLower = existing.map { it.lowercase() }.toMutableSet()
        incoming.forEach { tag ->
            if (seenLower.add(tag.lowercase())) {
                result.add(tag)
            }
        }
        return result
    }

    private fun guessMimeFromExtension(name: String): String {
        return when (name.substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "heic" -> "image/heic"
            "heif" -> "image/heif"
            else -> "image/*"
        }
    }

    private val naturalFileNameComparator = Comparator<ImageItem> { a, b ->
        compareNatural(a.name.lowercase(), b.name.lowercase())
    }

    private fun compareNatural(a: String, b: String): Int {
        var ia = 0
        var ib = 0
        while (ia < a.length && ib < b.length) {
            val ca = a[ia]
            val cb = b[ib]
            if (ca.isDigit() && cb.isDigit()) {
                val startA = ia
                val startB = ib
                while (ia < a.length && a[ia].isDigit()) ia++
                while (ib < b.length && b[ib].isDigit()) ib++
                val numA = a.substring(startA, ia).trimStart('0')
                val numB = b.substring(startB, ib).trimStart('0')
                if (numA.length != numB.length) return numA.length.compareTo(numB.length)
                val cmp = numA.compareTo(numB)
                if (cmp != 0) return cmp
            } else {
                if (ca != cb) return ca.compareTo(cb)
                ia++
                ib++
            }
        }
        return a.length.compareTo(b.length)
    }
}
