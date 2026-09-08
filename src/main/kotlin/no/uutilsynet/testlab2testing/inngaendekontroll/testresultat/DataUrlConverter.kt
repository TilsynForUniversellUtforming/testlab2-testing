package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64

@Component
class DataUrlConverter {

    fun dataUrlToImage(dataUrl: String): MultipartFile {
        require(dataUrl.startsWith("data:")) { "Ugyldig data-URL" }

        val commaIndex = dataUrl.indexOf(',')
        require(commaIndex > 0) { "Ugyldig data-URL: mangler separator" }

        val metadata = dataUrl.substring(5, commaIndex)
        val payload = dataUrl.substring(commaIndex + 1)
        val isBase64 = metadata.contains(";base64")
        val contentType = metadata.substringBefore(';').ifBlank { "application/octet-stream" }

        val bytes: ByteArray =
            decodeBytes(isBase64, payload)

        val extension =
            getExtension(contentType)

        val filename = "image-${Instant.now().toEpochMilli()}.$extension"

        return object : MultipartFile {
            override fun getName(): String = "file"

            override fun getOriginalFilename(): String = filename

            override fun getContentType(): String = contentType

            override fun isEmpty(): Boolean = bytes.isEmpty()

            override fun getSize(): Long = bytes.size.toLong()

            override fun getBytes(): ByteArray = bytes

            override fun getInputStream() = ByteArrayInputStream(bytes)

            override fun transferTo(dest: File) {
                dest.outputStream().use { it.write(bytes) }
            }
        }
    }

    private fun decodeBytes(isBase64: Boolean, payload: String): ByteArray {
        val bytes =
            if (isBase64) {
                Base64.getDecoder().decode(payload)
            } else {
                URLDecoder.decode(payload, StandardCharsets.UTF_8).toByteArray(StandardCharsets.UTF_8)
            }
        requireNotNull(bytes) { "Kunne ikkje dekode bytes frå data-URL" }
        return bytes
    }

    private fun getExtension(contentType: String): String {
        val extension =
            when (contentType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/gif" -> "gif"
                "image/webp" -> "webp"
                else -> "bin"
            }
        return extension
    }
}