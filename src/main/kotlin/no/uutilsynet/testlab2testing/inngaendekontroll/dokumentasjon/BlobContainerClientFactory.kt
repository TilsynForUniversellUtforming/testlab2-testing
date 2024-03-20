package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import com.azure.storage.blob.BlobContainerClient

interface BlobContainerClientFactory {
  fun createBlobContainerClient(): BlobContainerClient
}
