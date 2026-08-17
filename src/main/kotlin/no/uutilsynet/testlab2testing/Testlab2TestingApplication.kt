package no.uutilsynet.testlab2testing

import jakarta.validation.ClockProvider
import java.time.Clock
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.CommonsRequestLoggingFilter
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper

@SpringBootApplication(exclude = [SecurityAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableScheduling
@EnableCaching
class Testlab2TestingApplication {

  @Bean
  fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
    return restTemplateBuilder.requestFactory(::reqestFactory).build()
  }

  @Bean
  fun jsonMapper(): JsonMapper {
    return JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()
  }

  @Bean
  fun jacksonJsonHttpMessageConverter(jsonMapper: JsonMapper): JacksonJsonHttpMessageConverter {
    val converter = JacksonJsonHttpMessageConverter(jsonMapper)
    converter.supportedMediaTypes =
        listOf(
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_OCTET_STREAM,
        )
    return converter
  }

  @Bean
  fun commonsRequestLoggingFilter(): CommonsRequestLoggingFilter {
    val filter = CommonsRequestLoggingFilter()
    filter.setIncludeQueryString(true)
    filter.setIncludePayload(true)
    filter.setMaxPayloadLength(1000)
    return filter
  }

  @Bean
  fun reqestFactory(): BufferingClientHttpRequestFactory {
    return BufferingClientHttpRequestFactory(SimpleClientHttpRequestFactory())
  }

  @Bean
  fun clockProvider(): ClockProvider {
    return ClockProvider { Clock.systemDefaultZone() }
  }
}

fun main(args: Array<String>) {
  runApplication<Testlab2TestingApplication>(*args)
}
