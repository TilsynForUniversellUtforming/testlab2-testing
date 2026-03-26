package no.uutilsynet.testlab2testing.resultat.external.api

import no.uutilsynet.testlab2testing.resultat.export.ResultatRegisterProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

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
    return proxyFactory.createClient<TestresultatAggregertPerTestregelSearchControllerApi>(
    )
  }

  @Bean
  fun testresultatClient(): TestresultatSearchControllerApi {
    return proxyFactory.createClient<TestresultatSearchControllerApi>()
  }
}