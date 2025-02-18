package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import java.io.ByteArrayInputStream
import java.net.URI

interface ImageStorageClient {

  fun uploadToStorage(data: ByteArrayInputStream, fileName: String): Result<Unit>

  fun toBlobUri(filnamn: String, sasToken: String): URI

  fun getSasToken(): String

  fun deleteBilde(imagePath: String): Result<Boolean>

  fun restoreBilde(imagePath: String): Result<Unit>
}
