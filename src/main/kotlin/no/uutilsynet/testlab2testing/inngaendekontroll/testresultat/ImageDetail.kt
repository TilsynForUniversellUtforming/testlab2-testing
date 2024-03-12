package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.awt.image.BufferedImage

data class ImageDetail(
    val image: BufferedImage,
    val fileName: String,
    val fileExtension: String,
    val isThumbnail: Boolean
)
