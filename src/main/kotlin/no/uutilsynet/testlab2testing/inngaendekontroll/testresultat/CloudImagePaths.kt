package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.net.URI

data class CloudImagePaths(
    val bilde: String,
    val thumbnail: String,
)

data class CloudImageUris(val imageURI: URI, val thumbnailURI: URI)
