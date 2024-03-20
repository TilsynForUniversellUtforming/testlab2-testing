package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.awt.image.BufferedImage

data class BildeRequest(
    val image: BufferedImage,
    val thumbnail: BufferedImage,
    val fileName: String,
    val thumbnailName: String,
    val fileExtension: String
)
