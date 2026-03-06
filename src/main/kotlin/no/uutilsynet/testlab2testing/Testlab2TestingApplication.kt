package no.uutilsynet.testlab2testing

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.validation.ClockProvider
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.http.client.BufferingClientHttpRequestFactory
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestTemplate
import org.springframework.web.filter.CommonsRequestLoggingFilter
import java.time.Clock


@SpringBootApplication(
    exclude = [SecurityAutoConfiguration::class, HibernateJpaAutoConfiguration::class])
@ConfigurationPropertiesScan
@EnableScheduling
@EnableCaching
class Testlab2TestingApplication {

  @Bean
  fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
    val objectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      objectMapper.registerModule(JavaTimeModule())
    val mappingJackson2HttpMessageConverter = MappingJackson2HttpMessageConverter()
    mappingJackson2HttpMessageConverter.objectMapper = objectMapper
    mappingJackson2HttpMessageConverter.supportedMediaTypes =
        listOf(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM)

    return restTemplateBuilder
        .messageConverters(mappingJackson2HttpMessageConverter)
        .requestFactory(::reqestFactory)
        .build()
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
