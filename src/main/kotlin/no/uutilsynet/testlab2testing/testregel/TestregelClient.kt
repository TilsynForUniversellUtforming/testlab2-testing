package no.uutilsynet.testlab2testing.testregel

import no.uutilsynet.testlab2testing.testregel.krav.KravRegisterProperties
import no.uutilsynet.testlab2testing.testregel.model.InnhaldstypeTesting
import no.uutilsynet.testlab2testing.testregel.model.Tema
import no.uutilsynet.testlab2testing.testregel.model.Testregel
import no.uutilsynet.testlab2testing.testregel.model.TestregelKrav
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate

@Service
class TestregelClient(
    restTemplate: RestTemplate,
    private val kravregisterProperties: KravRegisterProperties
) {

    val restClient = RestClient.create(restTemplate)

    private val logger = LoggerFactory.getLogger(TestregelClient::class.java)




  fun getTestregelById(testregelId: Int): Result<Testregel>
  {
      return runCatching {
          restClient.get().uri("${kravregisterProperties.host}/v1/testrelar/$testregelId")
          .retrieve()
          .body(Testregel::class.java) ?: throw NoSuchElementException("Fant ikkje testregel med id $testregelId")
      }
  }

  fun getTestregelByKey(testregelKey: String): Result<Testregel> {
      return runCatching {
          restClient.get().uri("${kravregisterProperties.host}/v1/testrelar/testregelKey/$testregelKey")
              .retrieve()
              .body(Testregel::class.java) ?: throw NoSuchElementException("Fant ikkje testregel med nøkkel $testregelKey")
      }
  }

  fun getTestregelListFromIds(testregelIdList: List<Int>): List<Testregel> {
    return getTestregelList().getOrThrow().filter { testregelIdList.contains(it.id) }
  }

  fun getTestregelList(): Result<List<Testregel>> {
      return runCatching {
          restClient.get().uri("${kravregisterProperties.host}/v1/testrelar")
              .retrieve()
                .body(object : ParameterizedTypeReference<List<Testregel>>() {})
          ?: throw NoSuchElementException("Fant ingen testreglar")
      }
  }

    fun getTestregelKravList(): Result<List<TestregelKrav>> {
        return runCatching {
            restClient.get().uri("${kravregisterProperties.host}/v1/testrelar/listTestregelKrav")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<TestregelKrav>>() {})
                ?: throw NoSuchElementException("Fant ingen testreglar")
        }
    }

  fun getInnhaldstypeForTesting() : Result<List<InnhaldstypeTesting>> {
      return runCatching {
          restClient.get().uri("${kravregisterProperties.host}/v1/testrelar/innhaldstypeTesting")
              .retrieve()
              .body(object : ParameterizedTypeReference<List<InnhaldstypeTesting>>() {})
          ?: throw NoSuchElementException("Fant ingen innhaldstyper for testing")
      }
  }

  fun getTestregelByKravId(kravId: Int): List<TestregelKrav> {
    return getTestregelKravList().getOrThrow().filter { it.krav.id == kravId }
  }

    fun getTemaForTestregel() : Result<List<Tema>> {
        return runCatching {
            restClient.get().uri("${kravregisterProperties.host}/v1/testrelar/temaForTestreglar")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<Tema>>() {})
                ?: throw NoSuchElementException("Fant ingen innhaldstyper for testing")
        }
    }
}
