package no.uutilsynet.testlab2testing.resultat.external

import no.uutilsynet.testlab2testing.resultat.export.ResultatRegisterProperties
import no.uutilsynet.testlab2testing.resultat.external.api.TestresultatAggregertPerTestregelSearchControllerApi
import no.uutilsynet.testlab2testing.resultat.external.api.TestresultatSearchControllerApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class ResultatAppClientConfig(
    clientBuilder: RestClient.Builder,
    resultatRegisterProperties: ResultatRegisterProperties
) {
  val restClient = clientBuilder.baseUrl(resultatRegisterProperties.host).build()

  val proxyFactory: HttpServiceProxyFactory =
      HttpServiceProxyFactory.builder()
          .exchangeAdapter(RestClientAdapter.create(restClient))
          .build()

  @Bean
  fun testregelAggregertPerTestregelClient(): TestresultatAggregertPerTestregelSearchControllerApi {
    return proxyFactory.createClient(
        TestresultatAggregertPerTestregelSearchControllerApi::class.java)
  }

  @Bean
  fun testresultatClient(): TestresultatSearchControllerApi {
    return proxyFactory.createClient(TestresultatSearchControllerApi::class.java)
  }
}
