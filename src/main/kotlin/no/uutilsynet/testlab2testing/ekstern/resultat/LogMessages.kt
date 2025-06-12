package no.uutilsynet.testlab2testing.ekstern.resultat

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component

@Component
class LogMessages(@Autowired val messageSource: MessageSource) {

  fun teststNotFoundForOrgnr(orgnr: String): String? {
    return messageSource.getMessage(
        "ekstern.resultat.tests.notfound.orgnr",
        arrayOf(orgnr),
        "Fant ikkje publiserte testar for orgnr {0}",
        LocaleContextHolder.getLocale())
  }

  fun loeysingNotFoundForOrgnr(orgnr: String): String? {
    return messageSource.getMessage(
        "ekstern.resultat.loeysing.notfound.orgnr",
        arrayOf(orgnr),
        "Fant ingen løysingar for verkemd med søk {0}",
        LocaleContextHolder.getLocale())
  }

  fun verksemdMultipleFoundForSearch(orgnr: String): String? {
    return messageSource.getMessage(
        "ekstern.resultat.verksemd.multiple.found.orgnr",
        arrayOf(orgnr),
        "Fant fleire verksemder for søk etter {0}. Spesifiser søket nærmare",
        LocaleContextHolder.getLocale())
  }
}
