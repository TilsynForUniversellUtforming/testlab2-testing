package no.uutilsynet.testlab2testing.resultat.external

import no.uutilsynet.testlab2testing.resultat.export.ResultatRegisterProperties
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Service
class ResultatAppClient(
    restTemplate: RestTemplate,
    private val resultatRegisterProperties: ResultatRegisterProperties
) {

  val restClient = RestClient.create(restTemplate)

  var proxyFactory: HttpServiceProxyFactory? =
      HttpServiceProxyFactory.builder()
          .exchangeAdapter(RestClientAdapter.create(restClient))
          .build()
}
