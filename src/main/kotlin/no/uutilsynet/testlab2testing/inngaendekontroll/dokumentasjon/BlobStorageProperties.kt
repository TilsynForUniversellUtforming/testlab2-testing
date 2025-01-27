package no.uutilsynet.testlab2testing.inngaendekontroll.dokumentasjon

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "blobstorage")
data class BlobStorageProperties(
    val connection: String,
    val account: String,
    val container: String,
    val sasttl: Int,
    val eksternalhost: String,
)
