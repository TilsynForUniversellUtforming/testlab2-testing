package no.uutilsynet.testlab2testing.statistikk

import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BlobContainerClientFactory
import no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon.BlobStorageProperties
import org.springframework.stereotype.Component

@Component
class StatistikkDAO(
    blobStorageProperties: BlobStorageProperties,
    blobContainerClientFactory: BlobContainerClientFactory
) {

  private val blobContainerClient = blobContainerClientFactory.createBlobContainerClient()

  fun getDataFile(path: String) {
    blobContainerClient.getBlobClient(path).downloadToFile(path)
  }
}
