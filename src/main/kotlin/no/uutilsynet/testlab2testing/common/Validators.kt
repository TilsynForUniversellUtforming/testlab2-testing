package no.uutilsynet.testlab2testing.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.URL
import no.uutilsynet.testlab2testing.forenkletkontroll.Status

fun validateOrgNummer(s: String?): Result<String> = runCatching {
  requireNotNull(s) { "Organisasjonsnummer kan ikkje vere null" }
  require(s.all { it.isDigit() }) { "Organisasjonsnummer kan berre innehalde siffer" }
  require(s.length == 9) { "Organisasjonsnummer må vere 9 siffer" }

  val orgnummer = s.toCharArray().map { it.toString().toInt() }
  val vekter = listOf(3, 2, 7, 6, 5, 4, 3, 2)
  val sum = orgnummer.take(8).zip(vekter).sumOf { (a, b) -> a * b }
  val rest = sum % 11
  val kontrollsiffer = if (rest == 0) 0 else 11 - rest
  if (kontrollsiffer == orgnummer[8]) {
    s
  } else {
    throw IllegalArgumentException("$s er ikkje eit gyldig organisasjonsnummer")
  }
}

fun validateNamn(s: String?): Result<String> = runCatching {
  require(!(s == null || s == "")) { "mangler navn" }
  s
}

fun validateStatus(s: String?): Result<Status> =
    when (s) {
      "crawling" -> Result.success(Status.Crawling)
      "testing" -> Result.success(Status.Testing)
      else -> Result.failure(IllegalArgumentException("$s er ikke en gyldig status"))
    }

fun validateIdList(
    list: List<Int>?,
    validIds: List<Int>,
    parameterName: String
): Result<List<Int>> = runCatching {
  if (list.isNullOrEmpty()) {
    throw IllegalArgumentException(
        "Eg forventa eit parameter `${parameterName}` som skulle inneholde ei liste med id-ar, men han var tom.")
  }

  val invalidIds = list.filter { !validIds.contains(it) }
  if (invalidIds.isNotEmpty()) {
    throw IllegalArgumentException(
        "Id-ane ${invalidIds.joinToString(", ")} er ikkje gyldige. Gyldige id-ar er ${validIds.joinToString(", ")}.")
  }

  list
}

fun validateURL(s: String): Result<URL> = runCatching {
  val withProtocol =
      if (s.startsWith("http://") || s.startsWith("https://")) {
        s
      } else {
        "https://$s"
      }
  URI(withProtocol).toURL()
}

fun validateJSONString(s: String): Result<JsonNode> = runCatching {
  val mapper = ObjectMapper()
  mapper.readTree(s)
}
