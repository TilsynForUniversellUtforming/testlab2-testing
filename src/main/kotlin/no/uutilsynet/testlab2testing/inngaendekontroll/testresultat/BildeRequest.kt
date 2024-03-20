package no.uutilsynet.testlab2testing.inngaendekontroll.testresultat

import java.awt.image.BufferedImage

data class BildeRequest(
    val image: BufferedImage,
    val thumbnail: BufferedImage,
    val fileName: String,
    val fileExtension: String
) {
  val fullFileName: String
    get() = "${fileName}.${fileExtension}"

  val fullThumbnailName: String
    get() = "${fileName}_thumb.${fileExtension}"
}
